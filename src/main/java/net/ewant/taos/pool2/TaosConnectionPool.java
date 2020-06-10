package net.ewant.taos.pool2;

import net.ewant.taos.TaosConnection;
import net.ewant.taos.exception.TaosException;
import net.ewant.taos.support.TaosConfigProperties;
import net.ewant.taos.support.TaosPool2ConfigProperties;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class TaosConnectionPool extends TaosPool<TaosConnection>{

    public TaosConnectionPool(TaosConfigProperties configProperties, TaosPool2ConfigProperties pool2ConfigProperties) {
        this(pool2ConfigProperties, new TaosConnectionFactory(configProperties, pool2ConfigProperties));
    }

    public TaosConnectionPool(GenericObjectPoolConfig poolConfig, PooledObjectFactory<TaosConnection> factory) {
        super(poolConfig, factory);
    }

    @Override
    public TaosConnection getResource() {
        TaosConnection resource = super.getResource();
        if(resource != null){
            resource.setDataSource(this);
        }
        return resource;
    }

    @Override
    public void returnResource(final TaosConnection resource) {
        if (resource != null) {
            try {
                returnResourceObject(resource);
            } catch (Exception e) {
                returnBrokenResource(resource);
                throw new TaosException("Resource is returned to the pool as broken", e);
            }
        }
    }
}
