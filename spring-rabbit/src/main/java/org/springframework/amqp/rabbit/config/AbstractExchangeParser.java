/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.amqp.rabbit.config;

import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author Dave Syer
 * 
 */
public abstract class AbstractExchangeParser extends AbstractSingleBeanDefinitionParser {

	private static final String ARGUMENTS_ELEMENT = "exchange-arguments";

	private static final String DURABLE_ATTRIBUTE = "durable";

	private static final String AUTO_DELETE_ATTRIBUTE = "auto-delete";

	private static String BINDINGS_ELE = "bindings";

	private static String BINDING_ELE = "binding";

	protected static final String BINDING_QUEUE_ATTR = "queue";

	protected static final String BINDING_EXCHANGE_ATTR = "exchange";

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String exchangeName = element.getAttribute(NAME_ATTRIBUTE);
		builder.addConstructorArgValue(new TypedStringValue(exchangeName));
		parseBindings(element, parserContext, builder, exchangeName);

		NamespaceUtils.addConstructorArgBooleanValueIfAttributeDefined(builder, element, DURABLE_ATTRIBUTE, true);
		NamespaceUtils.addConstructorArgBooleanValueIfAttributeDefined(builder, element, AUTO_DELETE_ATTRIBUTE,
				false);

		Element argumentsElement = DomUtils.getChildElementByTagName(element, ARGUMENTS_ELEMENT);
		if (argumentsElement != null) {
			Map<?, ?> map = parserContext.getDelegate().parseMapElement(argumentsElement,
					builder.getRawBeanDefinition());
			builder.addConstructorArgValue(map);
		}

	}

	protected void parseBindings(Element element, ParserContext parserContext, BeanDefinitionBuilder builder,
			String exchangeName) {
		Element bindings = DomUtils.getChildElementByTagName(element, BINDINGS_ELE);
		doParseBindings(parserContext, exchangeName, bindings, this);
	}

	protected void doParseBindings(ParserContext parserContext,
			String exchangeName, Element bindings, AbstractExchangeParser parser) {
		if (bindings != null) {
			for (Element binding : DomUtils.getChildElementsByTagName(bindings, BINDING_ELE)) {
				AbstractBeanDefinition beanDefinition = parser.parseBinding(exchangeName, binding,
						parserContext);
				registerBeanDefinition(new BeanDefinitionHolder(beanDefinition, parserContext.getReaderContext()
						.generateBeanName(beanDefinition)), parserContext.getRegistry());
			}
		}
	}

	protected abstract AbstractBeanDefinition parseBinding(String exchangeName, Element binding,
			ParserContext parserContext);

	protected void parseDestination(Element binding, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String queueAttribute = binding.getAttribute(BINDING_QUEUE_ATTR);
		String exchangeAttribute = binding.getAttribute(BINDING_EXCHANGE_ATTR);
		boolean hasQueueAttribute = StringUtils.hasText(queueAttribute);
		boolean hasExchangeAttribute = StringUtils.hasText(exchangeAttribute);
		if (!(hasQueueAttribute ^ hasExchangeAttribute)) {
			parserContext.getReaderContext().error("Binding must have exactly one of 'queue' or 'exchange'", binding);
		}
		if (hasQueueAttribute) {
			builder.addPropertyReference("destinationQueue", queueAttribute);
		}
		if (hasExchangeAttribute) {
			builder.addPropertyReference("destinationExchange", exchangeAttribute);
		}
	}

}
