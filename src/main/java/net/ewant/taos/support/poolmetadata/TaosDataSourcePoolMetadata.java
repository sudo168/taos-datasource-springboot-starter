package net.ewant.taos.support.poolmetadata;

import net.ewant.taos.pool2.TaosDataSource;
import org.springframework.boot.jdbc.metadata.AbstractDataSourcePoolMetadata;

public class TaosDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<TaosDataSource> {

    public TaosDataSourcePoolMetadata(TaosDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Integer getActive() {
        return getDataSource().getPool().getNumActive();
    }

    @Override
    public Integer getMax() {
        return getDataSource().getPool().getMaxTotal();
    }

    @Override
    public Integer getMin() {
        return getDataSource().getPool().getMinIdle();
    }

    @Override
    public String getValidationQuery() {
        return getDataSource().getPool().getTestQuery();
    }

    @Override
    public Boolean getDefaultAutoCommit() {
        return true;
    }
}
