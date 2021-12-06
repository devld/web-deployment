package me.devld.wd.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import me.devld.wd.data.BaseException
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.context.request.WebRequest
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

    @Bean
    fun errorAttributes() = object : DefaultErrorAttributes() {
        override fun getErrorAttributes(
            webRequest: WebRequest?,
            options: ErrorAttributeOptions?
        ): MutableMap<String, Any> {
            val r = super.getErrorAttributes(webRequest, options)
            getError(webRequest)?.let {
                if (it is BaseException) {
                    r["error"] = it.reason
                }
            }
            return r
        }
    }
}
