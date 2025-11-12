package com.example.phantoms.data.local.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProductDao {

    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    fun getAll(): LiveData<List<ProductEntity>>

    // NEW: one-shot fetch of current cart (for order payload)
    @Query("SELECT * FROM cart_items ORDER BY id DESC")
    suspend fun getAllNow(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Update
    suspend fun update(vararg product: ProductEntity)

    // NEW: clear the cart
    @Query("DELETE FROM cart_items")
    suspend fun deleteAll()
}
