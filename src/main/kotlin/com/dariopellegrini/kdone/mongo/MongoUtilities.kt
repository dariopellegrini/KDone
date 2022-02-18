package com.dariopellegrini.kdone.mongo
import com.mongodb.client.model.Variable
import org.bson.conversions.Bson
import org.litote.kmongo.MongoOperator
import org.litote.kmongo.bson
import org.litote.kmongo.json
import org.litote.kmongo.lookup
import kotlin.reflect.KProperty

fun <T: Any>conditionalLookup(from: String, localField: T, foreignField: String, resultProperty: KProperty<Any?>, condition: Bson) =
    lookup(from = "platforms", let = listOf(Variable("localField", localField)), resultProperty = resultProperty, pipeline = arrayOf(
        """{ ${MongoOperator.match}:
            {
                ${MongoOperator.expr}: {
                ${MongoOperator.cond}: {
                if: { ${MongoOperator.isArray}: ["$${'$'}localField"] },
                then: { ${'$'}in: ["${'$'}$foreignField", "$${'$'}localField"] },
                else: {
                ${MongoOperator.cond}: {
                if: {${MongoOperator.isArray}: ["${'$'}$foreignField"] },
                then: { ${'$'}in: ["${'$'}${'$'}localField", "${'$'}$foreignField"] },
                else: { ${MongoOperator.eq}: ["${'$'}${'$'}localField", "${'$'}$foreignField"] }
            }
            }
            }
            },
            }
        }""".trimIndent().bson,
        condition
    ))

fun conditionalLookup(from: String, localField: String, foreignField: String, resultProperty: String, condition: Bson): Bson =
    """{
        ${'$'}lookup: {
        from: "$from",
        "let": { "localField": "${'$'}$localField"},
        pipeline: [

        { ${MongoOperator.match}:
            {
                ${MongoOperator.expr}: {
                ${MongoOperator.cond}: {
                if: { ${MongoOperator.isArray}: ["$${'$'}localField"] },
                then: { ${'$'}in: ["${'$'}$foreignField", "$${'$'}localField"] },
                else: {
                    ${MongoOperator.cond}: {
                        if: {${MongoOperator.isArray}: ["${'$'}$foreignField"] },
                        then: { ${'$'}in: ["${'$'}${'$'}localField", "${'$'}$foreignField"] },
                        else: { ${MongoOperator.eq}: ["${'$'}${'$'}localField", "${'$'}$foreignField"] }
                    }
                }
            }
            },
            }
        },
        ${condition.json}
        ],
        as: "$resultProperty"
    }
    }""".trimIndent().bson
