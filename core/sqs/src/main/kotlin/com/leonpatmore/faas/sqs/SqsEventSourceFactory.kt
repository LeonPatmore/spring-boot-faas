package com.leonpatmore.faas.sqs

import com.fasterxml.jackson.databind.ObjectMapper
import com.leonpatmore.fass.common.EVENT_SOURCE_ENABLED_PROPERTY_PREFIX
import com.leonpatmore.fass.common.FunctionSourceData
import com.leonpatmore.fass.common.Handler
import com.leonpatmore.fass.common.HandlerEventSourceFactory
import com.leonpatmore.fass.common.Message
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Component
@ConditionalOnProperty(EVENT_SOURCE_ENABLED_PROPERTY_PREFIX + "sqs.enabled")
class SqsEventSourceFactory(
    private val sqsAsyncClient: SqsAsyncClient,
    private val objectMapper: ObjectMapper,
    private val registry: MessageListenerContainerRegistry,
) : HandlerEventSourceFactory<SqsProperties> {
    override fun wrapHandler(data: FunctionSourceData<SqsProperties>) {
        val container = createContainer(data)
        container.start()
        registry.registerListenerContainer(container)
        data.context.registerBean(data.functionName + "SqsListenerContainer", SqsMessageListenerContainer::class.java, container)
    }

    override fun getPropertyClass(): Class<SqsProperties> = SqsProperties::class.java

    private fun createContainer(data: FunctionSourceData<SqsProperties>) =
        SqsMessageListenerContainerFactory<String>()
            .apply { this.setSqsAsyncClient(sqsAsyncClient) }
            .createContainer(data.properties.queueName).apply {
                this.id = "${data.functionName}Container"
            }
            .withHandler(data.handler)

    private fun <T> SqsMessageListenerContainer<String>.withHandler(handler: Handler<T>): SqsMessageListenerContainer<String> {
        this.setMessageListener {
            LOGGER.info("Received raw sqs message [ ${it.payload} ]")
            val messageBody: T =
                if (handler.getMessageType() == String::class.java) {
                    it.payload as T
                } else {
                    objectMapper.readValue(it.payload, handler.getMessageType())
                }
            handler.handle(Message(messageBody))
        }
        return this
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(SqsEventSourceFactory::class.java)
    }
}
