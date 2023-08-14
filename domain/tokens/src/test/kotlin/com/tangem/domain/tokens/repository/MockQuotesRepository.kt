package com.tangem.domain.tokens.repository

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.domain.core.error.DataError
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Quote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class MockQuotesRepository(
    private val quotes: Flow<Either<DataError, Set<Quote>>>,
) : QuotesRepository {

    override fun getQuotes(currenciesIds: Set<CryptoCurrency.ID>, refresh: Boolean): Flow<Set<Quote>> {
        return quotes.map { it.getOrElse { e -> throw e } }
    }
}
