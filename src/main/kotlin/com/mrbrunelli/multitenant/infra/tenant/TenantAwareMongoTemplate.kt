package com.mrbrunelli.multitenant.infra.tenant

import com.mrbrunelli.multitenant.domain.annotation.TenantId
import com.mongodb.client.result.DeleteResult
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.convert.MongoConverter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.util.concurrent.ConcurrentHashMap

class TenantAwareMongoTemplate(
    mongoDatabaseFactory: MongoDatabaseFactory,
    mongoConverter: MongoConverter,
) : MongoTemplate(mongoDatabaseFactory, mongoConverter) {

    private val tenantFieldCache = ConcurrentHashMap<Class<*>, String?>()

    private fun resolveTenantFieldName(entityClass: Class<*>): String? =
        tenantFieldCache.computeIfAbsent(entityClass) {
            it.declaredFields
                .firstOrNull { field -> field.isAnnotationPresent(TenantId::class.java) }
                ?.let { field ->
                    field.getAnnotation(org.springframework.data.mongodb.core.mapping.Field::class.java)?.value
                        ?: field.name
                }
        }

    private fun addTenantCriteria(query: Query, entityClass: Class<*>) {
        val fieldName = resolveTenantFieldName(entityClass) ?: return
        val tenantId = try {
            TenantContext.get()
        } catch (_: IllegalStateException) {
            return
        }
        if (!query.queryObject.containsKey(fieldName)) {
            query.addCriteria(Criteria.where(fieldName).`is`(tenantId))
        }
    }

    override fun <T : Any> find(query: Query, entityClass: Class<T>, collectionName: String): List<T> {
        addTenantCriteria(query, entityClass)
        return super.find(query, entityClass, collectionName)
    }

    override fun <T : Any> findOne(query: Query, entityClass: Class<T>, collectionName: String): T? {
        addTenantCriteria(query, entityClass)
        return super.findOne(query, entityClass, collectionName)
    }

    override fun <T : Any> findById(id: Any, entityClass: Class<T>, collectionName: String): T? {
        val fieldName = resolveTenantFieldName(entityClass) ?: return super.findById(id, entityClass, collectionName)
        val tenantId = try {
            TenantContext.get()
        } catch (_: IllegalStateException) {
            return super.findById(id, entityClass, collectionName)
        }
        val query = Query(Criteria.where("_id").`is`(id).and(fieldName).`is`(tenantId))
        return super.findOne(query, entityClass, collectionName)
    }

    override fun count(query: Query, entityClass: Class<*>?, collectionName: String): Long {
        if (entityClass != null) addTenantCriteria(query, entityClass)
        return super.count(query, entityClass, collectionName)
    }

    override fun exists(query: Query, entityClass: Class<*>?, collectionName: String): Boolean {
        if (entityClass != null) addTenantCriteria(query, entityClass)
        return super.exists(query, entityClass, collectionName)
    }

    override fun <T : Any> doRemove(
        collectionName: String,
        query: Query,
        entityClass: Class<T>?,
        multi: Boolean
    ): DeleteResult {
        if (entityClass != null) addTenantCriteria(query, entityClass)
        return super.doRemove(collectionName, query, entityClass, multi)
    }
}
