package me.devld.wd.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * WebConfig
 *
 * @author devld
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.removeIf { hc: HttpMessageConverter<*>? -> hc is StringHttpMessageConverter }
        converters.removeIf { hc: HttpMessageConverter<*>? -> hc is MappingJackson2HttpMessageConverter }
        converters.add(MappingJackson2HttpMessageConverter(objectMapper()))
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        val om = ObjectMapper()
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        om.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        return om
    }
}
