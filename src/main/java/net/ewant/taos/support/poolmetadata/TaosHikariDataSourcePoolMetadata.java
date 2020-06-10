package net.ewant.taos.support.poolmetadata;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.ewant.taos.pool2.TaosDataSource;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.jdbc.metadata.AbstractDataSourcePoolMetadata;

public class TaosHikariDataSourcePoolMetadata extends AbstractDataSourcePoolMetadata<HikariDataSource> {

    public TaosHikariDataSourcePoolMetadata(HikariDataSource dataSource) {
        super(dataSource);
    }

    public Integer getActive() {
        try {
            return this.getHikariPool().getActiveConnections();
        } catch (Exception var2) {
            return null;
        }
    }

    private HikariPool getHikariPool() {
        return (HikariPool)(new DirectFieldAccessor(this.getDataSource())).getPropertyValue("pool");
    }

    public Integer getMax() {
        return (this.getDataSource()).getMaximumPoolSize();
    }

    public Integer getMin() {
        return (this.getDataSource()).getMinimumIdle();
    }

    public String getValidationQuery() {
        return (this.getDataSource()).getConnectionTestQuery();
    }

    public Boolean getDefaultAutoCommit() {
        return (this.getDataSource()).isAutoCommit();
    }
}
