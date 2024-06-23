package org.apache.flink.dynamic.json

import org.apache.flink.cep.pattern.{Pattern, Quantifier}
import org.apache.flink.dynamic.impl.json.serde.{ConditionSpecStdDeserializer, NodeSpecStdDeserializer, PatternTimesStdDeserializer, PatternTimesStdSerializer}
import org.apache.flink.dynamic.impl.json.spec.{ConditionSpec, GraphSpec, NodeSpec}
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.ser.DurationSerializer

import java.time.Duration

/**
 * CepConverter
 *
 * @author chaoxin.lu
 * @version V 1.0
 * @since 2024-06-20
 */
object CepConverter {
  private val MAPPER: ObjectMapper = new ObjectMapper()
    .registerModule(new SimpleModule()
      .addDeserializer(classOf[ConditionSpec], ConditionSpecStdDeserializer.INSTANCE)
      .addDeserializer(classOf[Duration], DurationDeserializer.INSTANCE)
      .addDeserializer(classOf[NodeSpec], NodeSpecStdDeserializer.INSTANCE)
      .addDeserializer(classOf[Quantifier.Times], PatternTimesStdDeserializer.INSTANCE)
      .addSerializer(DurationSerializer.INSTANCE)
      .addSerializer(PatternTimesStdSerializer.INSTANCE))

  /**
   * toJSONString
   *
   * @param pattern
   * @return
   */
  def toJSONString[T, F <: T](pattern: Pattern[T, F]): String = {
    val graphSpec = GraphSpec.fromPattern(pattern)
    MAPPER.writeValueAsString(graphSpec)
  }

  /**
   * toPattern
   *
   * @param jsonString
   * @tparam T
   * @tparam F
   * @return
   */
  def toPattern[T, F <: T](jsonString: String): Pattern[T, F] = toPattern(jsonString, Thread.currentThread.getContextClassLoader)

  /**
   * toPattern
   *
   * @param jsonString
   * @tparam T
   * @tparam F
   * @return
   */
  def toPattern[T, F <: T](jsonString: String, userCodeClassLoader: ClassLoader): Pattern[T, F] = {
    if (userCodeClassLoader == null) {
      return toPattern(jsonString)
    }
    try {
      val deserializedGraphSpec: GraphSpec = MAPPER.readValue(jsonString, classOf[GraphSpec])
      deserializedGraphSpec.toPattern(userCodeClassLoader)
    } catch {
      case e: Exception => throw new IllegalStateException(e)
    }
  }

  /**
   * toGraphSpec
   *
   * @param jsonString
   * @return
   */
  def toGraphSpec(jsonString: String): GraphSpec = MAPPER.readValue(jsonString, classOf[GraphSpec])

  /**
   * toJSONString
   *
   * @param graphSpec
   * @return
   */
  def toJSONString(graphSpec: GraphSpec): String = MAPPER.writeValueAsString(graphSpec)

}
