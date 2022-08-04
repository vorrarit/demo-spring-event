package com.example.demospringevent

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.event.EventListener
import org.springframework.data.repository.CrudRepository
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class DemoSpringEventApplication

fun main(args: Array<String>) {
	runApplication<DemoSpringEventApplication>(*args)
}

@RestController
@RequestMapping("/api/v1/demo")
class DemoController {

	companion object {
		val log = LoggerFactory.getLogger(DemoController::class.java)
	}

	@Autowired
	private lateinit var customerService: CustomerService

	@Autowired
	private lateinit var publisher: ApplicationEventPublisher

	@GetMapping("/event/basic")
	fun eventBasic() {
		publisher.publishEvent(CustomEvent(this, "event-basic"))
	}

	@PostMapping("/customer")
	fun createCustomer(@RequestBody customer: Customer) {
		customerService.createCustomer(customer)
	}
}

data class CustomEvent(val source: Any, val message: String)

@Component
class CustomEventListener {
	companion object {
		val log = LoggerFactory.getLogger(CustomEventListener::class.java)
	}

	@EventListener
	fun handleEvent(event: CustomEvent) {
		log.info("custom event: {}", event.message)
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	fun handleTransactionalEvent(event: CustomEvent) {
		log.info("custom event after commit: {}", event.message)
	}
}

data class Customer(var id: String, var name: String) {
	var token: String? = null
}

@Repository
class CustomerRepository {
	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	fun createCustomer(customer: Customer) {
		val sql = "insert into customer (id, name, token) values (?, ?, ?)"
		jdbcTemplate.update(sql, customer.id, customer.name, customer.token)
	}
}

@Service
class CustomerService {

	companion object {
		val log = LoggerFactory.getLogger(CustomerService::class.java)
	}

	@Autowired
	private lateinit var publisher: ApplicationEventPublisher

	@Autowired
	private lateinit var customerRepository: CustomerRepository

	@Transactional
	fun createCustomer(customer: Customer) {
		log.info("publish event")
		publisher.publishEvent(CustomEvent(this, "customer-created"))
		Thread.sleep(5000)
		log.info("create customer")
		customerRepository.createCustomer(customer)
	}

}