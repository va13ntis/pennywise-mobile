package com.pennywise.app.presentation.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency

/**
 * Represents an item in the currency dropdown - either a currency or a section header
 */
sealed class CurrencyDropdownItem {
    data class CurrencyItem(val currency: Currency) : CurrencyDropdownItem()
    data class SectionHeader(val title: String) : CurrencyDropdownItem()
}

/**
 * A reusable currency selection component that extends MaterialAutoCompleteTextView
 * with search functionality and grouping by popularity.
 */
class CurrencySelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialAutoCompleteTextView(context, attrs, defStyleAttr) {
    
    private var currencies: List<Currency> = Currency.values().toList()
    private var popularCurrencies: List<Currency> = Currency.getMostPopular()
    private var onCurrencySelectedListener: ((Currency) -> Unit)? = null
    private var currencyAdapter: CurrencyAdapter? = null
    
    init {
        setupView()
    }
    
    /**
     * Initialize the view with default settings
     */
    private fun setupView() {
        // Set hint text
        hint = context.getString(R.string.select_currency)
        
        // Set minimum dropdown height
        threshold = 1
        
        // Set up accessibility
        setupAccessibility()
        
        // Create and set adapter with grouping
        currencyAdapter = CurrencyAdapter(context, currencies, popularCurrencies)
        setAdapter(currencyAdapter)
        
        // Set item click listener
        setOnItemClickListener { _, _, position, _ ->
            val item = currencyAdapter?.getItem(position)
            if (item is CurrencyDropdownItem.CurrencyItem) {
                // Announce selection for accessibility
                announceCurrencySelection(item.currency)
                onCurrencySelectedListener?.invoke(item.currency)
            }
        }
    }
    
    /**
     * Set up accessibility features
     */
    private fun setupAccessibility() {
        // Set important for accessibility
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        
        // Set content description with accessibility hint
        val hint = context.getString(R.string.currency_selection_hint)
        contentDescription = "${context.getString(R.string.select_currency)}. $hint"
        
        // Enable keyboard navigation
        isFocusable = true
        isFocusableInTouchMode = true
    }
    
    /**
     * Set the listener for currency selection events
     */
    fun setOnCurrencySelectedListener(listener: (Currency) -> Unit) {
        onCurrencySelectedListener = listener
    }
    
    /**
     * Set the list of currencies to display
     */
    fun setCurrencies(currencies: List<Currency>) {
        this.currencies = currencies
        currencyAdapter = CurrencyAdapter(context, currencies, popularCurrencies)
        setAdapter(currencyAdapter)
    }
    
    /**
     * Set the list of popular currencies for grouping
     */
    fun setPopularCurrencies(popularCurrencies: List<Currency>) {
        this.popularCurrencies = popularCurrencies
        currencyAdapter = CurrencyAdapter(context, currencies, popularCurrencies)
        setAdapter(currencyAdapter)
    }
    
    /**
     * Set the selected currency programmatically
     */
    fun setSelectedCurrency(currencyCode: String) {
        val currency = Currency.fromCode(currencyCode) ?: Currency.getDefault()
        setText(Currency.getDisplayText(currency), false)
    }
    
    /**
     * Get the currently selected currency
     */
    fun getSelectedCurrency(): Currency? {
        val text = text.toString()
        return currencies.find { currency ->
            Currency.getDisplayText(currency) == text
        }
    }
    
    /**
     * Announce currency selection for accessibility
     */
    private fun announceCurrencySelection(currency: Currency) {
        val announcement = "${currency.displayName} selected"
        announceForAccessibility(announcement)
    }
    
    /**
     * Override to handle keyboard navigation
     */
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_DPAD_CENTER,
            android.view.KeyEvent.KEYCODE_ENTER -> {
                // Open dropdown on center/enter key
                showDropDown()
                true
            }
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Open dropdown on down arrow
                showDropDown()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    /**
     * Custom adapter for currency dropdown with grouping functionality
     */
    private inner class CurrencyAdapter(
        context: Context,
        private val currencies: List<Currency>,
        private val popularCurrencies: List<Currency>
    ) : ArrayAdapter<CurrencyDropdownItem>(context, 0, emptyList()) {
        
        private var filteredItems: List<CurrencyDropdownItem> = emptyList()
        private val filter = CurrencyFilter()
        
        init {
            filteredItems = createGroupedItems()
        }
        
        private fun createGroupedItems(): List<CurrencyDropdownItem> {
            val items = mutableListOf<CurrencyDropdownItem>()
            
            // Add popular currencies section
            if (popularCurrencies.isNotEmpty()) {
                items.add(CurrencyDropdownItem.SectionHeader("Most Popular"))
                popularCurrencies.forEach { currency ->
                    items.add(CurrencyDropdownItem.CurrencyItem(currency))
                }
            }
            
            // Add all currencies section
            val otherCurrencies = currencies.filter { it !in popularCurrencies }
            if (otherCurrencies.isNotEmpty()) {
                items.add(CurrencyDropdownItem.SectionHeader("All Currencies"))
                otherCurrencies.forEach { currency ->
                    items.add(CurrencyDropdownItem.CurrencyItem(currency))
                }
            }
            
            return items
        }
        
        override fun getCount(): Int = filteredItems.size
        
        override fun getItem(position: Int): CurrencyDropdownItem = filteredItems[position]
        
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            
            return when (item) {
                is CurrencyDropdownItem.SectionHeader -> {
                    val view = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.item_currency_section_header, parent, false)
                    val headerText = view.findViewById<TextView>(android.R.id.text1)
                    headerText.text = item.title
                    
                    // Set accessibility properties for section header
                    view.contentDescription = item.title
                    view.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    
                    view
                }
                is CurrencyDropdownItem.CurrencyItem -> {
                    val view = convertView ?: LayoutInflater.from(context)
                        .inflate(R.layout.item_currency_selection, parent, false)
                    
                    val currency = item.currency
                    val currencyCode = view.findViewById<TextView>(R.id.currency_code)
                    val currencySymbol = view.findViewById<TextView>(R.id.currency_symbol)
                    val currencyName = view.findViewById<TextView>(R.id.currency_name)
                    
                    currencyCode.text = currency.code
                    currencySymbol.text = currency.symbol
                    currencyName.text = currency.displayName
                    
                    // Set accessibility properties for currency item
                    val accessibilityText = "${currency.displayName}, ${currency.code}, ${currency.symbol}"
                    view.contentDescription = accessibilityText
                    view.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
                    
                    // Make the entire item clickable for accessibility
                    view.isClickable = true
                    view.isFocusable = true
                    
                    view
                }
            }
        }
        
        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is CurrencyDropdownItem.SectionHeader -> 0
                is CurrencyDropdownItem.CurrencyItem -> 1
            }
        }
        
        override fun getViewTypeCount(): Int = 2
        
        override fun getFilter(): Filter = filter
        
        /**
         * Custom filter for currency search functionality
         */
        private inner class CurrencyFilter : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                
                if (constraint.isNullOrBlank()) {
                    results.values = createGroupedItems()
                    results.count = createGroupedItems().size
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    val filtered = currencies.filter { currency ->
                        currency.code.lowercase().contains(filterPattern) ||
                        currency.symbol.lowercase().contains(filterPattern) ||
                        currency.displayName.lowercase().contains(filterPattern)
                    }
                    
                    val filteredItems = mutableListOf<CurrencyDropdownItem>()
                    if (filtered.isNotEmpty()) {
                        filteredItems.add(CurrencyDropdownItem.SectionHeader("Search Results"))
                        filtered.forEach { currency ->
                            filteredItems.add(CurrencyDropdownItem.CurrencyItem(currency))
                        }
                    }
                    
                    results.values = filteredItems
                    results.count = filteredItems.size
                }
                
                return results
            }
            
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<CurrencyDropdownItem> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
