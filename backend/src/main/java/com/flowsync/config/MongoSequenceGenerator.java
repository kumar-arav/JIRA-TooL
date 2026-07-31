package com.flowsync.config;

import com.flowsync.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
public class MongoSequenceGenerator {

    private final MongoOperations mongoOperations;

    public long generateSequence(String seqName) {
        DatabaseSequence counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                DatabaseSequence.class
        );
        return counter != null ? counter.getSeq() : 1;
    }

    @Component
    @RequiredArgsConstructor
    public static class BaseEntityBeforeConvertListener extends AbstractMongoEventListener<BaseEntity> {
        private final MongoSequenceGenerator sequenceGenerator;

        @Override
        public void onBeforeConvert(BeforeConvertEvent<BaseEntity> event) {
            BaseEntity entity = event.getSource();
            if (entity.getId() == null) {
                entity.setId(sequenceGenerator.generateSequence(entity.getClass().getSimpleName() + "_sequence"));
            }
        }
    }
}
