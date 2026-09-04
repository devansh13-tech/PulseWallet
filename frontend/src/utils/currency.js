import { useEffect, useState } from 'react'

export const CURRENCY_STORAGE_KEY = 'pulsewallet_currency'
export const CURRENCY_EVENT = 'pulsewallet_currency_change'

export const SUPPORTED_CURRENCIES = [
    { code: 'INR', name: 'Indian Rupee', symbol: '₹' },
    { code: 'USD', name: 'US Dollar', symbol: '$' },
    { code: 'EUR', name: 'Euro', symbol: '€' },
    { code: 'GBP', name: 'British Pound', symbol: '£' },
    { code: 'JPY', name: 'Japanese Yen', symbol: '¥' },
    { code: 'CAD', name: 'Canadian Dollar', symbol: 'C$' },
    { code: 'AUD', name: 'Australian Dollar', symbol: 'A$' },
    { code: 'AED', name: 'UAE Dirham', symbol: 'د.إ' },
]

export const DEFAULT_CURRENCY = 'INR'
export const BASE_CURRENCY = 'INR'
export const EXCHANGE_RATE_CACHE_KEY = 'pulsewallet_exchange_rates'
export const EXCHANGE_RATE_UPDATED_AT_KEY = 'pulsewallet_exchange_rates_updated_at'

export function isSupportedCurrency(currencyCode) {
    return SUPPORTED_CURRENCIES.some((currency) => currency.code === currencyCode)
}

export function getStoredCurrency() {
    if (typeof window === 'undefined') return DEFAULT_CURRENCY
    const saved = window.localStorage.getItem(CURRENCY_STORAGE_KEY)
    return isSupportedCurrency(saved) ? saved : DEFAULT_CURRENCY
}

export function setStoredCurrency(currencyCode) {
    const normalized = isSupportedCurrency(currencyCode) ? currencyCode : DEFAULT_CURRENCY
    if (typeof window !== 'undefined') {
        window.localStorage.setItem(CURRENCY_STORAGE_KEY, normalized)
        window.dispatchEvent(new CustomEvent(CURRENCY_EVENT, { detail: { currency: normalized } }))
    }
    return normalized
}

export function getCurrencyMeta(currencyCode = getStoredCurrency()) {
    return SUPPORTED_CURRENCIES.find((currency) => currency.code === currencyCode) ?? SUPPORTED_CURRENCIES[0]
}

export function getStoredExchangeRates() {
    if (typeof window === 'undefined') return null
    try {
        const raw = window.localStorage.getItem(EXCHANGE_RATE_CACHE_KEY)
        return raw ? JSON.parse(raw) : null
    } catch {
        return null
    }
}

export function setStoredExchangeRates(rates) {
    if (typeof window === 'undefined') return
    window.localStorage.setItem(EXCHANGE_RATE_CACHE_KEY, JSON.stringify(rates))
    window.localStorage.setItem(EXCHANGE_RATE_UPDATED_AT_KEY, String(Date.now()))
}

export function getLastExchangeRateUpdate() {
    if (typeof window === 'undefined') return null
    const raw = window.localStorage.getItem(EXCHANGE_RATE_UPDATED_AT_KEY)
    if (!raw) return null
    return Number(raw)
}

export function convertAmountToCurrency(value, fromCurrency = BASE_CURRENCY, toCurrency = getStoredCurrency()) {
    const amount = Number(value ?? 0)
    if (!Number.isFinite(amount)) return 0
    if (!fromCurrency || !toCurrency || fromCurrency === toCurrency) return amount

    const cachedRates = getStoredExchangeRates() || {}
    const rateForBase = cachedRates[fromCurrency] || 1
    const rateForTarget = cachedRates[toCurrency] || 1

    if (!rateForBase || !rateForTarget || rateForBase === 0) return amount

    const rate = (rateForTarget / rateForBase)
    return amount * rate
}

export function formatMoneyAmount(value, currencyCode = getStoredCurrency(), fromCurrency = BASE_CURRENCY) {
    const numeric = convertAmountToCurrency(value, fromCurrency, currencyCode)
    const formatter = new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: currencyCode,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    })
    return formatter.format(numeric)
}

export function formatNumber(value) {
    const numeric = Number(value ?? 0)
    if (!Number.isFinite(numeric)) return '0'
    return new Intl.NumberFormat('en-US').format(numeric)
}

export function useSelectedCurrency() {
    const [currency, setCurrency] = useState(() => getStoredCurrency())

    useEffect(() => {
        const handleStorage = () => setCurrency(getStoredCurrency())
        const handleCurrencyChange = (event) => {
            const nextCurrency = event?.detail?.currency ?? getStoredCurrency()
            setCurrency(nextCurrency)
        }

        window.addEventListener('storage', handleStorage)
        window.addEventListener(CURRENCY_EVENT, handleCurrencyChange)

        return () => {
            window.removeEventListener('storage', handleStorage)
            window.removeEventListener(CURRENCY_EVENT, handleCurrencyChange)
        }
    }, [])

    return currency
}
