package none.nintha.vtraceapi.config

import com.mongodb.MongoClientURI
import com.mongodb.WriteConcern
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.core.WriteConcernResolver
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext


@Suppress("DEPRECATION")
@Configuration
class MongoConfig {
    @Value("@{mongo.url}")
    lateinit var mongoUrl: String

    @Bean
    fun mongoDbFactory(): MongoDbFactory {
        return SimpleMongoDbFactory(MongoClientURI(mongoUrl))
    }

    @Bean
    fun mongoTemplate(): MongoTemplate {
        //remove _class
        val converter = MappingMongoConverter(mongoDbFactory(), MongoMappingContext())
        converter.setTypeMapper(DefaultMongoTypeMapper(null))

        return MongoTemplate(mongoDbFactory(), converter)
    }

    @Bean
    fun writeConcernResolver(): WriteConcernResolver {
        return WriteConcernResolver { _ -> WriteConcern.UNACKNOWLEDGED }
    }
}