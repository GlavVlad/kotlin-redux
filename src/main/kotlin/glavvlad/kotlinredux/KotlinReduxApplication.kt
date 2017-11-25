package glavvlad.kotlinredux

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.support.beans
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class KotlinReduxApplication

fun main(args: Array<String>) {
//    runApplication<KotlinReduxApplication>(*args)
    SpringApplicationBuilder()
            .initializers(beans {
                bean { SpringTransactionManager(ref()) }
                bean {
                    ApplicationRunner {
                        val customerService = ref<CustomerService>()
                        arrayOf("Vlad", "Julia", "Ivan", "Daria", "Dima")
                                .map { Customer(name = it) }
                                .forEach { customerService.create(it) }
                        customerService.getAll().forEach { println(it) }
                    }
                }
            })
            .sources(KotlinReduxApplication::class.java)
            .run(*args)
}

@RestController
class CustomerRestController(private val customerService: CustomerService) {
    @GetMapping("customers")
    fun customer() = this.customerService.getAll()
}

@Service
@Transactional
class ExposedCustomerService(private val transactionTemplate: TransactionTemplate) : CustomerService, InitializingBean {
    override fun afterPropertiesSet() {
        this.transactionTemplate.execute {
            SchemaUtils.create(Customers)
        }
    }

    override fun getAll(): Collection<Customer> = Customers
            .selectAll()
            .map { Customer(it[Customers.name], it[Customers.id]) }

    override fun findOne(id: Long): Customer? = Customers
            .select { Customers.id.eq(id) }
            .map { Customer(it[Customers.name], it[Customers.id]) }
            .firstOrNull()

    override fun create(customer: Customer) {
        Customers.insert { it[Customers.name] = customer.name }
    }
}

object Customers : Table() {
    val id = long("ID").autoIncrement().primaryKey()
    val name = varchar("NAME", 255)
}

//@Service
//@Transactional
//class JdbcTemplateCustomerService(private val jdbcTemplate: JdbcTemplate) : CustomerService {
//    override fun getAll(): Collection<Customer> = this.jdbcTemplate.query("SELECT * FROM CUSTOMERS") { rs, _ ->
//        Customer(rs.getString("NAME"), rs.getLong("ID"))
//    }
//
//    override fun findOne(id: Long): Customer? = this.jdbcTemplate.queryForObject("SELECT * FROM CUSTOMERS WHERE ID = ?", id) { rs, _ ->
//        Customer(rs.getString("NAME"), rs.getLong("ID"))
//    }
//
//    override fun create(customer: Customer) {
//        this.jdbcTemplate.execute("INSERT INTO CUSTOMERS(NAME) VALUES(?)") {
//            it.setString(1, customer.name)
//            it.execute()
//        }
//    }
//
//}

interface CustomerService {
    fun getAll(): Collection<Customer>
    fun findOne(id: Long): Customer?
    fun create(customer: Customer)
}

data class Customer(val name: String, var id: Long? = null)