package com.paysera.lib.accounts.clients

import com.paysera.lib.accounts.entities.Balance
import com.paysera.lib.accounts.entities.IbanInformation
import com.paysera.lib.accounts.interfaces.TokenRefresherInterface
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import com.paysera.lib.accounts.retrofit.APIClient
import retrofit2.HttpException
import java.util.concurrent.TimeUnit.SECONDS

class AccountsApiClient(private val apiClient: APIClient, private val tokenRefresherInterface: TokenRefresherInterface) {

    private val retryCondition = { errors: Observable<Throwable> ->
        errors.flatMap {
            val isUnauthorized = (it as HttpException).code() == 401
            if (isUnauthorized) {
                synchronized (tokenRefresherInterface) {
                    if (tokenRefresherInterface.isRefreshing()) {
                        Observable.timer(1, SECONDS).subscribeOn(Schedulers.io())
                    } else {
                        tokenRefresherInterface.refreshToken()
                    }
                }
            } else {
                Observable.error(it)
            }
        }
    }

    fun getIbanInformation(iban: String): Observable<IbanInformation> {
        return apiClient.getIbanInformation(iban).retryWhen(retryCondition)
    }

    fun getFullBalance(accountNumber: String): Observable<List<Balance>> {
        return apiClient.getFullBalances(accountNumber).retryWhen(retryCondition)
    }
}