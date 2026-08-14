package dev.lyy.zelchatcross.redis;

import dev.lyy.zelchatcross.ZelChatCross;
import dev.lyy.zelchatcross.config.ConfigManager;
import dev.lyy.zelchatcross.scheduler.TaskHandle;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages Redis connection pooling (JedisPool), auto-reconnect, and Pub/Sub lifecycle.
 */
public final class RedisManager {

    private final ZelChatCross plugin;
    private final ConfigManager config;
    private final RedisChannel channels;

    private JedisPool pool;
    private RedisPublisher publisher;
    private RedisSubscriber subscriber;
    private volatile boolean connected = false;
    private TaskHandle reconnectTask;

    public RedisManager(ZelChatCross plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.channels = new RedisChannel(config.getRedisChannelPrefix());
    }

    public synchronized boolean connect() {
        disconnect(false);

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(config.getRedisPoolMaxTotal());
            poolConfig.setMaxIdle(config.getRedisPoolMaxIdle());
            poolConfig.setMinIdle(config.getRedisPoolMinIdle());
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
            poolConfig.setBlockWhenExhausted(true);
            poolConfig.setMaxWait(Duration.ofMillis(config.getRedisTimeoutMs()));

            String user = config.getRedisUsername();
            String pass = config.getRedisPassword();

            DefaultJedisClientConfig.Builder clientConfigBuilder = DefaultJedisClientConfig.builder()
                    .connectionTimeoutMillis(config.getRedisTimeoutMs())
                    .socketTimeoutMillis(config.getRedisTimeoutMs())
                    .database(config.getRedisDatabase())
                    .ssl(config.isRedisSsl());

            if (user != null && !user.trim().isEmpty()) {
                clientConfigBuilder.user(user.trim());
            }
            if (pass != null && !pass.trim().isEmpty()) {
                clientConfigBuilder.password(pass.trim());
            }

            HostAndPort hostAndPort = new HostAndPort(config.getRedisHost(), config.getRedisPort());
            this.pool = new JedisPool(poolConfig, hostAndPort.getHost(), hostAndPort.getPort(),
                    config.getRedisTimeoutMs(),
                    (user != null && !user.isEmpty()) ? user : null,
                    (pass != null && !pass.isEmpty()) ? pass : null,
                    config.getRedisDatabase(),
                    null,
                    config.isRedisSsl());

            // Test connection
            try (Jedis jedis = pool.getResource()) {
                String ping = jedis.ping();
                if (!"PONG".equalsIgnoreCase(ping)) {
                    plugin.getLogger().warning("[Redis] Ping response unexpected: " + ping);
                }
            }

            this.publisher = new RedisPublisher(plugin, this);
            this.subscriber = new RedisSubscriber(plugin, this);
            this.subscriber.start();

            this.connected = true;
            stopReconnectTask();

            plugin.getLogger().info("[Redis] Connected successfully to Redis/DragonflyDB at "
                    + config.getRedisHost() + ":" + config.getRedisPort() + " (DB " + config.getRedisDatabase() + ")");
            return true;
        } catch (Exception e) {
            this.connected = false;
            plugin.getLogger().warning("[Redis] Could not connect to Redis/DragonflyDB at "
                    + config.getRedisHost() + ":" + config.getRedisPort() + " (" + e.getMessage() + ").");
            startReconnectTask();
            return false;
        }
    }

    public synchronized void disconnect() {
        disconnect(true);
    }

    private synchronized void disconnect(boolean stopReconnect) {
        this.connected = false;

        if (stopReconnect) {
            stopReconnectTask();
        }

        if (subscriber != null) {
            try {
                subscriber.stop();
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[Redis] Error stopping subscriber: " + e.getMessage());
            }
            subscriber = null;
        }

        if (pool != null && !pool.isClosed()) {
            try {
                pool.close();
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE, "[Redis] Error closing JedisPool: " + e.getMessage());
            }
            pool = null;
        }
    }

    private synchronized void startReconnectTask() {
        if (reconnectTask == null) {
            plugin.getLogger().info("[Redis] Background reconnect task started. Retrying every 10s...");
            this.reconnectTask = plugin.getScheduler().runAsyncRepeating(() -> {
                if (!connected) {
                    connect();
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    private synchronized void stopReconnectTask() {
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
    }

    /**
     * Gets a Jedis instance from the pool. Make sure to use try-with-resources.
     *
     * @return a Jedis connection
     */
    public Jedis getResource() {
        if (pool == null || pool.isClosed()) {
            throw new IllegalStateException("JedisPool is closed or not initialized");
        }
        return pool.getResource();
    }

    public boolean isConnected() {
        return connected && pool != null && !pool.isClosed();
    }

    public RedisChannel getChannels() {
        return channels;
    }

    public RedisPublisher getPublisher() {
        return publisher;
    }

    public RedisSubscriber getSubscriber() {
        return subscriber;
    }
}
