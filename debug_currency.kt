import kotlin.Double.Companion.NaN

fun main() {
    val result = Currency.formatAmount(NaN, Currency.USD)
    println("Result: '$result'")
    println("Expected: '\$NaN'")
    println("Are equal: ${result == "\$NaN"}")
}
