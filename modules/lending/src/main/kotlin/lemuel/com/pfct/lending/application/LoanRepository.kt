package lemuel.com.pfct.lending.application

import lemuel.com.pfct.lending.domain.Loan
import lemuel.com.pfct.lending.domain.LoanId

interface LoanRepository {
    fun existsById(id: LoanId): Boolean
    fun findById(id: LoanId): Loan?
    fun save(loan: Loan): Loan
}
