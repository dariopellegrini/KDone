package com.dariopellegrini.kdone.mongo

import auth.mongoId
import com.dariopellegrini.kdone.exceptions.NotFoundException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexModel
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.changestream.FullDocument
import com.mongodb.client.result.DeleteResult
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.*
import kotlin.collections.toList

class MongoRepository<T>(private val database: MongoDatabase,
                         private val collectionName: String,
                         private val classType: Class<T>) {
    private val collection = database.getCollection(collectionName, classType)

    suspend fun insert(element: T) = withContext(Dispatchers.IO) {
        collection.insertOne(element)
    }

    suspend fun findAll(): List<T> = withContext(Dispatchers.IO) {
        collection.find().toList()
    }

    suspend fun findAll(bson: Bson, limit: Int? = null, skip: Int? = null): List<T> = withContext(Dispatchers.IO) {
        collection.find(bson).apply {
            limit?.let { limit(it) }
            skip?.let { skip(it) }
        }.toList()
    }

    suspend fun findAll(vararg bson: Bson, limit: Int? = null, skip: Int? = null): List<T> = withContext(Dispatchers.IO) {
        collection.find(*bson).apply {
            limit?.let { limit(it) }
            skip?.let { skip(it) }
        }.toList()
    }

    suspend fun findAll(json: String, limit: Int? = null, skip: Int? = null): List<T> = withContext(Dispatchers.IO) {
        collection.find(json).apply {
            limit?.let { limit(it) }
            skip?.let { skip(it) }
        }.toList()
    }

    suspend fun findById(id: String): T = withContext(Dispatchers.IO) {
        collection.findOneById(id.mongoId<T>()) ?: throw NotFoundException()
    }

    suspend fun findById(id: Id<T>): T = withContext(Dispatchers.IO) {
        collection.findOneById(id) ?: throw NotFoundException()
    }

    suspend fun findOne(bson: Bson): T = withContext(Dispatchers.IO) {
        collection.findOne(bson) ?: throw NotFoundException()
    }

    suspend fun findOne(vararg bson: Bson): T = withContext(Dispatchers.IO) {
        collection.findOne(*bson) ?: throw NotFoundException()
    }

    suspend fun findOne(json: String): T = withContext(Dispatchers.IO) {
        collection.findOne(json) ?: throw NotFoundException()
    }

    suspend fun findOneOrNull(bson: Bson): T? = withContext(Dispatchers.IO) {
        collection.findOne(bson)
    }

    suspend fun findOneOrNull(vararg bson: Bson): T? = withContext(Dispatchers.IO) {
        collection.findOne(*bson)
    }

    suspend fun findOneOrNull(json: String): T? = withContext(Dispatchers.IO) {
        collection.findOne(json)
    }

    suspend fun deleteById(id: Id<T>): DeleteResult = withContext(Dispatchers.IO) {
        collection.deleteOneById(id)
    }

    suspend fun deleteOne(bson: Bson): DeleteResult = withContext(Dispatchers.IO) {
        collection.deleteOne(bson)
    }

    suspend fun deleteOne(vararg bson: Bson): DeleteResult = withContext(Dispatchers.IO) {
        collection.deleteOne(*bson)
    }

    suspend fun deleteOne(json: String): DeleteResult = withContext(Dispatchers.IO) {
        collection.deleteOne(json)
    }

    suspend fun deleteMany(bson: Bson): DeleteResult = withContext(Dispatchers.IO) {
        collection.deleteMany(bson)
    }

    suspend fun updateOneById(id: Id<T>, update: Bson): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateOneById(id, update)
    }

    suspend fun updateOneById(id: Id<T>, update: Any): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateOneById(id, update, UpdateOptions())
    }

    suspend fun updateOne(bson: Bson, update: Bson): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateOne(bson, update)
    }

    suspend fun updateOne(json: String, update: Any): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateOne(json, update, UpdateOptions())
    }

    suspend fun updateOne(bson: Bson, update: Any): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateOne(bson, update, UpdateOptions())
    }

    suspend fun updateMany(bson: Bson, update: Bson): UpdateResult = withContext(Dispatchers.IO) {
        collection.updateMany(bson, update)
    }

    suspend fun aggregate(json: List<String>): List<T> = withContext(Dispatchers.IO) {
        collection.aggregate(json.map { Document.parse(it) }, classType).toList()
    }

    suspend fun aggregate(vararg bson: Bson): List<T> = withContext(Dispatchers.IO) {
        collection.aggregate(bson.toList(), classType).toList()
    }

    suspend fun aggregateBsonList(bsonList: List<Bson>): List<T> = withContext(Dispatchers.IO) {
        collection.aggregate(bsonList, classType).toList()
    }

    suspend fun count(): Long = withContext(Dispatchers.IO) {
        collection.countDocuments()
    }

    suspend fun count(bson: Bson): Long = withContext(Dispatchers.IO) {
        collection.countDocuments(bson)
    }

    suspend fun count(json: String): Long = withContext(Dispatchers.IO) {
        collection.countDocuments(json)
    }

    fun createIndex(index: String) {
        collection.createIndex(index.bson)
    }
}