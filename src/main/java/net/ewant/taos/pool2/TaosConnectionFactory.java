package net.ewant.taos.pool2;

import net.ewant.taos.TaosConnection;
import net.ewant.taos.exception.TaosConnectionException;
import net.ewant.taos.support.TaosConfigProperties;
import net.ewant.taos.support.TaosPool2ConfigProperties;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.sql.*;

public class TaosConnectionFactory implements PooledObjectFactory<TaosConnection> {

    private TaosConfigProperties configProperties;
    private TaosPool2ConfigProperties pool2ConfigProperties;

    public TaosConnectionFactory(TaosConfigProperties configProperties, TaosPool2ConfigProperties pool2ConfigProperties){
        this.configProperties = configProperties;
        this.pool2ConfigProperties = pool2ConfigProperties;
    }

    @Override
    public PooledObject<TaosConnection> makeObject() throws Exception {
        // Creates an instance that can be served by the pool
        try {
            Connection realConnect = DriverManager.getConnection(configProperties.getJdbcUrl(), configProperties.getProperties());
            return new DefaultPooledObject(new TaosConnection(realConnect));
        } catch (Exception e) {
            throw new TaosConnectionException("Can not connect to server.", e);
        }
    }

    @Override
    public void destroyObject(PooledObject<TaosConnection> pooledObject) throws Exception {
        // Destroys an instance no longer needed by the pool.
        TaosConnection connection = pooledObject.getObject();
        if(connection != null && !connection.isClosed()){
            try {
                connection.getRealConnection().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<TaosConnection> pooledObject) {
        // Ensures that the instance is safe to be returned by the pool.
        TaosConnection connection = pooledObject.getObject();
        try {
            if(connection == null || connection.isClosed()){
                return false;
            }
        } catch (SQLException e) {
            return false;
        }

        try (Statement statement = connection.createStatement()){
            ResultSet resultSet = statement.executeQuery(pool2ConfigProperties.getTestQuery());
            if(resultSet != null){
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    @Override
    public void activateObject(PooledObject<TaosConnection> pooledObject) throws Exception {
        // Reinitializes an instance to be returned by the pool.
    }

    @Override
    public void passivateObject(PooledObject<TaosConnection> pooledObject) throws Exception {
        // Uninitializes an instance to be returned to the idle object pool.
    }
}
