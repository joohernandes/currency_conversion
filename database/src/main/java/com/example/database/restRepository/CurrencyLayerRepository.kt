package com.example.database.restRepository

import android.content.Context
import com.example.database.Repository
import com.example.database.cacheRepository.PersistenceRepository
import com.example.database.dataModel.CurrencyItem
import com.example.database.dataModel.ExchangeRate
import com.example.database.dataModel.toCurrencyList
import com.example.database.dataModel.toExchangeList
import com.example.database.restRepository.client.RestServiceImpl
import com.example.database.restRepository.client.WebClient
import io.reactivex.Single
import java.util.*

class CurrencyLayerRepository(private val context: Context) : Repository {

    private val restService by lazy { RestServiceImpl(WebClient()).provideWebService() }

    override fun fetchAvailableCurrencies(): Single<List<CurrencyItem>> =
        restService.getCurrencyList()
            .map { it.toCurrencyList() }

    override fun fetchRates(currencyCode: String): Single<List<ExchangeRate>> {
        val cache = PersistenceRepository(context)
        return cache.getCachedExchange(currencyCode)
            .flatMap {
                val lastSave = Date(it.timestamp)
                when {
                    Date().before(lastSave.addTime(Calendar.MINUTE, 30)) -> cache.getCachedRates(
                        currencyCode
                    )
                    else -> getRatesFromService(currencyCode)
                }
            }
    }

    private fun getRatesFromService(currencyCode: String): Single<List<ExchangeRate>> =
        restService.getExchangeRates(currencyCode)
            .doOnSuccess {
                val cache = PersistenceRepository(context)
                cache.persistData(it)
            }
            .map { it.toExchangeList() }
}

private fun Date.addTime(field: Int, amount: Int): Date {
    val cal = Calendar.getInstance()
    cal.time = this
    cal.add(field, amount)
    return cal.time
}