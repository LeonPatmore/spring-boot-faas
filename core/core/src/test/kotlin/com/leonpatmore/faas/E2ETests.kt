package com.leonpatmore.faas

import com.leonpatmore.faas.common.TestHandlerConfiguration
import com.leonpatmore.fass.common.EventTarget
import com.leonpatmore.fass.common.FunctionSourceData
import com.leonpatmore.fass.common.Handler
import com.leonpatmore.fass.common.HandlerEventSourceFactory
import com.leonpatmore.fass.common.HandlerEventTargetFactory
import com.leonpatmore.fass.common.Message
import com.leonpatmore.fass.common.Response
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.support.registerBean
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
    properties = [
        "functions.test.source.props.requiredProp=abc",
        "functions.test.handler=stringTestHandler",
    ],
)
@ContextConfiguration(classes = [TestConfig::class, TestHandlerConfiguration::class])
class E2ETests {
    @Autowired
    private lateinit var eventSource: TestEventSource

    @Test
    fun name() {
        eventSource.produce() shouldBe "res"
    }
}

class TestEventSource(private val handler: Handler<*>) {
    fun produce(): Any {
        val typedHandler = handler as Handler<String>
        val res = typedHandler.handle(Message("test message"))
        return res.body
    }
}

data class TestEventSourceProperties(val requiredProp: String, val optional: String = "hello")

class TestEventSourceFactory : HandlerEventSourceFactory<TestEventSourceProperties> {
    override fun wrapHandler(data: FunctionSourceData<TestEventSourceProperties>) {
        val testEventSource = TestEventSource(data.handler)
        data.context.registerBean {
            testEventSource
        }
    }

    override fun getPropertyClass() = TestEventSourceProperties::class.java
}

class TestEventTarget : EventTarget {
    override fun handle(res: Response) {
        println("Event target for message ${res.body} reached")
    }
}

class TestEventTargetFactory : HandlerEventTargetFactory {
    override fun generateTarget(): EventTarget {
        return TestEventTarget()
    }
}

@TestConfiguration
class TestConfig {
    @Bean
    fun testEventSourceFactory() = TestEventSourceFactory()

    @Bean
    fun testEventTargetFactory() = TestEventTargetFactory()
}
