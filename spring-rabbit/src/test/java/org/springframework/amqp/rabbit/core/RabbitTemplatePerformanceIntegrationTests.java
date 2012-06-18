package org.springframework.amqp.rabbit.core;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.test.BrokerRunning;
import org.springframework.amqp.rabbit.test.BrokerTestUtils;
import org.springframework.amqp.rabbit.test.Log4jLevelAdjuster;
import org.springframework.amqp.rabbit.test.RepeatProcessor;
import org.springframework.test.annotation.Repeat;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class RabbitTemplatePerformanceIntegrationTests {

	private static final String ROUTE = "test.queue";

	private RabbitTemplate template = new RabbitTemplate();

	@Rule
	public RepeatProcessor repeat = new RepeatProcessor(4);

	@Rule
	// After the repeat processor, so it only runs once
	public Log4jLevelAdjuster logLevels = new Log4jLevelAdjuster(Level.ERROR, RabbitTemplate.class);

	@Rule
	// After the repeat processor, so it only runs once
	public BrokerRunning brokerIsRunning = BrokerRunning.isRunningWithEmptyQueues(ROUTE);

	private CachingConnectionFactory connectionFactory;

	@Before
	public void declareQueue() {
		if (repeat.isInitialized()) {
			// Important to prevent concurrent re-initialization
			return;
		}
		connectionFactory = new CachingConnectionFactory();
		connectionFactory.setChannelCacheSize(repeat.getConcurrency());
		connectionFactory.setPort(BrokerTestUtils.getPort());
		template.setConnectionFactory(connectionFactory);
	}

	@After
	public void cleanUp() {
		if (repeat.isInitialized()) {
			return;
		}
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
	}

	@Test
	@Repeat(2000)
	public void testSendAndReceive() throws Exception {
		template.convertAndSend(ROUTE, "message");
		String result = (String) template.receiveAndConvert(ROUTE);
		int count = 5;
		while (result == null && count-- > 0) {
			/*
			 * Retry for the purpose of non-transacted case because channel operations are async in that case
			 */
			Thread.sleep(10L);
			result = (String) template.receiveAndConvert(ROUTE);
		}
		assertEquals("message", result);
	}

	@Test
	@Repeat(2000)
	public void testSendAndReceiveTransacted() throws Exception {
		template.setChannelTransacted(true);
		template.convertAndSend(ROUTE, "message");
		String result = (String) template.receiveAndConvert(ROUTE);
		assertEquals("message", result);
	}

	@Test
	@Repeat(2000)
	public void testSendAndReceiveExternalTransacted() throws Exception {
		template.setChannelTransacted(true);
		new TransactionTemplate(new TestTransactionManager()).execute(new TransactionCallback<Void>() {
			public Void doInTransaction(TransactionStatus status) {
				template.convertAndSend(ROUTE, "message");
				return null;
			}
		});
		template.convertAndSend(ROUTE, "message");
		String result = (String) template.receiveAndConvert(ROUTE);
		assertEquals("message", result);
	}

	@SuppressWarnings("serial")
	private class TestTransactionManager extends AbstractPlatformTransactionManager {

		@Override
		protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		}

		@Override
		protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		}

		@Override
		protected Object doGetTransaction() throws TransactionException {
			return new Object();
		}

		@Override
		protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		}

	}

}
