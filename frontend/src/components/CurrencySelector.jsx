import {
    SUPPORTED_CURRENCIES,
    getCurrencyMeta,
    setStoredCurrency,
    useSelectedCurrency,
} from '../utils/currency'

export default function CurrencySelector() {
    const currency = useSelectedCurrency()
    const selectedCurrency = getCurrencyMeta(currency)

    const handleChange = (event) => {
        setStoredCurrency(event.target.value)
    }

    return (
        <label className="currency-selector" htmlFor="currency-selector">
            <span className="currency-selector-label">Display currency</span>
            <select
                id="currency-selector"
                value={currency}
                onChange={handleChange}
                aria-label="Select display currency"
            >
                {SUPPORTED_CURRENCIES.map((option) => (
                    <option key={option.code} value={option.code}>
                        {option.symbol} {option.code} — {option.name}
                    </option>
                ))}
            </select>
            <span className="currency-selector-current" aria-live="polite">
                {selectedCurrency.symbol} {selectedCurrency.code}
            </span>
        </label>
    )
}
