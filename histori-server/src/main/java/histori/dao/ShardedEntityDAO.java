package histori.dao;

import histori.dao.internal.ShardDAO;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public abstract class ShardedEntityDAO<E extends Shardable, D extends SingleShardDAO<E>> extends AbstractShardedDAO<E, D> {

    @Autowired private HistoriConfiguration configuration;

    @Getter private ShardDAO shardDAO;
    @Autowired public void setShardDAO (ShardDAO shardDAO) {
        this.shardDAO = shardDAO;
        initAllDAOs();
    }

    @Override protected DatabaseConfiguration getMasterDbConfiguration() { return configuration.getDatabase(); }

    public E findByName(String name) { return findByUniqueField(getNameField(), name, true); }
    public E findByName(String name, boolean useCache) { return findByUniqueField(getNameField(), name, useCache); }

    public String getNameField() { return "name"; }

}
