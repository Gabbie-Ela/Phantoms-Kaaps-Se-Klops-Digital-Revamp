package com.example.phantoms.data.local.room

import androidx.lifecycle.LiveData

class CartRepo(private val productDao: ProductDao) {

    val allCartProducts: LiveData<List<ProductEntity>> = productDao.getAll()

    suspend fun insert(product: ProductEntity) = productDao.insert(product)

    suspend fun delete(product: ProductEntity) = productDao.delete(product)

    suspend fun update(product: ProductEntity) = productDao.update(product)

    // NEW
    suspend fun clearAll() = productDao.deleteAll()

    // NEW
    suspend fun getAllNow(): List<ProductEntity> = productDao.getAllNow()
}
