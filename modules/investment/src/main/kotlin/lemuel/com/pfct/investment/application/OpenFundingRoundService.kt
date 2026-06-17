package lemuel.com.pfct.investment.application

import lemuel.com.pfct.investment.domain.FundingRound
import lemuel.com.pfct.investment.domain.FundingRoundId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 펀딩 라운드 개설 유스케이스. */
@Service
class OpenFundingRoundService(
    private val repository: FundingRoundRepository,
) {
    @Transactional
    fun open(command: OpenFundingRoundCommand): FundingRoundId {
        val id = FundingRoundId(command.id)
        require(repository.findById(id) == null) { "이미 존재하는 펀딩 라운드입니다: ${command.id}" }
        repository.save(FundingRound(id, command.targetAmount))
        return id
    }
}
