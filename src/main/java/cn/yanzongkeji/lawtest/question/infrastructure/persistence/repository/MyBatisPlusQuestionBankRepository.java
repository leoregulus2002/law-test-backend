package cn.yanzongkeji.lawtest.question.infrastructure.persistence.repository;

import cn.yanzongkeji.lawtest.question.domain.model.*;
import cn.yanzongkeji.lawtest.question.domain.port.QuestionBankRepository;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.dataobject.QuestionBankDO;
import cn.yanzongkeji.lawtest.question.infrastructure.persistence.mapper.QuestionBankMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class MyBatisPlusQuestionBankRepository implements QuestionBankRepository {
    private final QuestionBankMapper mapper;

    public ImportResult createIfAbsent(QuestionBank bank) {
        QuestionBankDO data = toDO(bank);
        Long id = mapper.insertIfAbsent(data);
        if (id != null)
            return new ImportResult(new QuestionBankId(id), true);
        return new ImportResult(new QuestionBankId(mapper.selectIdByCode(bank.code().value())), false);
    }

    public Optional<QuestionBank> findById(QuestionBankId id) {
        return Optional.ofNullable(mapper.selectById(id.value())).map(this::toDomain);
    }

    public List<QuestionBank> findPage(int offset, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<QuestionBankDO>().orderByDesc(QuestionBankDO::getId)
                .last("limit " + limit + " offset " + offset)).stream().map(this::toDomain).toList();
    }

    public long count() {
        return mapper.selectCount(null);
    }

    public boolean deleteById(QuestionBankId id) {
        return mapper.deleteById(id.value()) > 0;
    }

    private QuestionBankDO toDO(QuestionBank bank) {
        QuestionBankDO d = new QuestionBankDO();
        d.setCode(bank.code().value());
        d.setName(bank.name());
        d.setSourceFileName(bank.sourceFileName());
        return d;
    }

    private QuestionBank toDomain(QuestionBankDO d) {
        return QuestionBank.reconstitute(new QuestionBankId(d.getId()), new QuestionBankCode(d.getCode()), d.getName(),
                d.getSourceFileName());
    }
}
