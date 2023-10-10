package net.unit8.spring.idempotency.storage.inmemory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A map decorator that decorates another map and expires key-value pairs
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 * @see org.apache.commons.collections4.map.PassiveExpiringMap
 */
public class PassiveExpiringMap<K, V>
        extends AbstractMap<K, V>
        implements Serializable {

    /**
     * A {@link org.apache.commons.collections4.map.PassiveExpiringMap.ExpirationPolicy ExpirationPolicy}
     * that returns an expiration time that is a
     * constant about of time in the future from the current time.
     *
     * @param <K> the type of the keys in the map
     * @param <V> the type of the values in the map
     * @since 4.0
     */
    public static class ConstantTimeToLiveExpirationPolicy<K, V>
            implements ExpirationPolicy<K, V> {

        /** Serialization version */
        private static final long serialVersionUID = 1L;

        /** the constant time-to-live value measured in milliseconds. */
        private final long timeToLiveMillis;

        /**
         * Default constructor. Constructs a policy using a negative
         * time-to-live value that results in entries never expiring.
         */
        public ConstantTimeToLiveExpirationPolicy() {
            this(-1L);
        }

        /**
         * Construct a policy with the given time-to-live constant measured in
         * milliseconds. A negative time-to-live value indicates entries never
         * expire. A zero time-to-live value indicates entries expire (nearly)
         * immediately.
         *
         * @param timeToLiveMillis the constant amount of time (in milliseconds)
         *        an entry is available before it expires. A negative value
         *        results in entries that NEVER expire. A zero value results in
         *        entries that ALWAYS expire.
         */
        public ConstantTimeToLiveExpirationPolicy(final long timeToLiveMillis) {
            this.timeToLiveMillis = timeToLiveMillis;
        }

        /**
         * Construct a policy with the given time-to-live constant measured in
         * the given time unit of measure.
         *
         * @param timeToLive the constant amount of time an entry is available
         *        before it expires. A negative value results in entries that
         *        NEVER expire. A zero value results in entries that ALWAYS
         *        expire.
         * @param timeUnit the unit of time for the {@code timeToLive}
         *        parameter, must not be null.
         * @throws NullPointerException if the time unit is null.
         */
        public ConstantTimeToLiveExpirationPolicy(final long timeToLive,
                                                  final TimeUnit timeUnit) {
            this(validateAndConvertToMillis(timeToLive, timeUnit));
        }

        /**
         * Determine the expiration time for the given key-value entry.
         *
         * @param key the key for the entry (ignored).
         * @param value the value for the entry (ignored).
         * @return if {@link #timeToLiveMillis} &ge; 0, an expiration time of
         *         {@link #timeToLiveMillis} +
         *         {@link System#currentTimeMillis()} is returned. Otherwise, -1
         *         is returned indicating the entry never expires.
         */
        @Override
        public long expirationTime(final K key, final V value) {
            if (timeToLiveMillis >= 0L) {
                // avoid numerical overflow
                final long nowMillis = System.currentTimeMillis();
                if (nowMillis > Long.MAX_VALUE - timeToLiveMillis) {
                    // expiration would be greater than Long.MAX_VALUE
                    // never expire
                    return -1;
                }

                // timeToLiveMillis in the future
                return nowMillis + timeToLiveMillis;
            }

            // never expire
            return -1L;
        }
    }

    /**
     * A policy to determine the expiration time for key-value entries.
     *
     * @param <K> the key object type.
     * @param <V> the value object type
     * @since 4.0
     */
    @FunctionalInterface
    public interface ExpirationPolicy<K, V>
            extends Serializable {

        /**
         * Determine the expiration time for the given key-value entry.
         *
         * @param key the key for the entry.
         * @param value the value for the entry.
         * @return the expiration time value measured in milliseconds. A
         *         negative return value indicates the entry never expires.
         */
        long expirationTime(K key, V value);
    }

    /** Serialization version */
    private static final long serialVersionUID = 1L;

    /**
     * First validate the input parameters. If the parameters are valid, convert
     * the given time measured in the given units to the same time measured in
     * milliseconds.
     *
     * @param timeToLive the constant amount of time an entry is available
     *        before it expires. A negative value results in entries that NEVER
     *        expire. A zero value results in entries that ALWAYS expire.
     * @param timeUnit the unit of time for the {@code timeToLive}
     *        parameter, must not be null.
     * @throws NullPointerException if the time unit is null.
     */
    private static long validateAndConvertToMillis(final long timeToLive,
                                                   final TimeUnit timeUnit) {
        Objects.requireNonNull(timeUnit, "timeUnit");
        return TimeUnit.MILLISECONDS.convert(timeToLive, timeUnit);
    }

    /** The map to decorate */
    transient Map<K, V> map;

    /** map used to manage expiration times for the actual map entries. */
    private final Map<Object, Long> expirationMap = new HashMap<>();

    /** the policy used to determine time-to-live values for map entries. */
    private final ExpirationPolicy<K, V> expiringPolicy;

    /**
     * Default constructor. Constructs a map decorator that results in entries
     * NEVER expiring.
     */
    public PassiveExpiringMap() {
        this(-1L);
    }

    /**
     * Construct a map decorator using the given expiration policy to determine
     * expiration times.
     *
     * @param expiringPolicy the policy used to determine expiration times of
     *        entries as they are added.
     * @throws NullPointerException if expiringPolicy is null
     */
    public PassiveExpiringMap(final ExpirationPolicy<K, V> expiringPolicy) {
        this(expiringPolicy, new HashMap<>());
    }

    /**
     * Construct a map decorator that decorates the given map and uses the given
     * expiration policy to determine expiration times. If there are any
     * elements already in the map being decorated, they will NEVER expire
     * unless they are replaced.
     *
     * @param expiringPolicy the policy used to determine expiration times of
     *        entries as they are added.
     * @param map the map to decorate, must not be null.
     * @throws NullPointerException if the map or expiringPolicy is null.
     */
    public PassiveExpiringMap(final ExpirationPolicy<K, V> expiringPolicy,
                              final Map<K, V> map) {
        this.map = map;
        this.expiringPolicy = Objects.requireNonNull(expiringPolicy, "expiringPolicy");
    }

    /**
     * Construct a map decorator that decorates the given map using the given
     * time-to-live value measured in milliseconds to create and use a
     * {@link ConstantTimeToLiveExpirationPolicy} expiration policy.
     *
     * @param timeToLiveMillis the constant amount of time (in milliseconds) an
     *        entry is available before it expires. A negative value results in
     *        entries that NEVER expire. A zero value results in entries that
     *        ALWAYS expire.
     */
    public PassiveExpiringMap(final long timeToLiveMillis) {
        this(new ConstantTimeToLiveExpirationPolicy<>(timeToLiveMillis),
                new HashMap<>());
    }

    /**
     * Construct a map decorator using the given time-to-live value measured in
     * milliseconds to create and use a
     * {@link ConstantTimeToLiveExpirationPolicy} expiration policy. If there
     * are any elements already in the map being decorated, they will NEVER
     * expire unless they are replaced.
     *
     * @param timeToLiveMillis the constant amount of time (in milliseconds) an
     *        entry is available before it expires. A negative value results in
     *        entries that NEVER expire. A zero value results in entries that
     *        ALWAYS expire.
     * @param map the map to decorate, must not be null.
     * @throws NullPointerException if the map is null.
     */
    public PassiveExpiringMap(final long timeToLiveMillis, final Map<K, V> map) {
        this(new ConstantTimeToLiveExpirationPolicy<>(timeToLiveMillis),
                map);
    }

    /**
     * Construct a map decorator using the given time-to-live value measured in
     * the given time units of measure to create and use a
     * {@link ConstantTimeToLiveExpirationPolicy} expiration policy.
     *
     * @param timeToLive the constant amount of time an entry is available
     *        before it expires. A negative value results in entries that NEVER
     *        expire. A zero value results in entries that ALWAYS expire.
     * @param timeUnit the unit of time for the {@code timeToLive}
     *        parameter, must not be null.
     * @throws NullPointerException if the time unit is null.
     */
    public PassiveExpiringMap(final long timeToLive, final TimeUnit timeUnit) {
        this(validateAndConvertToMillis(timeToLive, timeUnit));
    }

    /**
     * Construct a map decorator that decorates the given map using the given
     * time-to-live value measured in the given time units of measure to create
     * {@link ConstantTimeToLiveExpirationPolicy} expiration policy. This policy
     * is used to determine expiration times. If there are any elements already
     * in the map being decorated, they will NEVER expire unless they are
     * replaced.
     *
     * @param timeToLive the constant amount of time an entry is available
     *        before it expires. A negative value results in entries that NEVER
     *        expire. A zero value results in entries that ALWAYS expire.
     * @param timeUnit the unit of time for the {@code timeToLive}
     *        parameter, must not be null.
     * @param map the map to decorate, must not be null.
     * @throws NullPointerException if the map or time unit is null.
     */
    public PassiveExpiringMap(final long timeToLive, final TimeUnit timeUnit, final Map<K, V> map) {
        this(validateAndConvertToMillis(timeToLive, timeUnit), map);
    }

    /**
     * Constructs a map decorator that decorates the given map and results in
     * entries NEVER expiring. If there are any elements already in the map
     * being decorated, they also will NEVER expire.
     *
     * @param map the map to decorate, must not be null.
     * @throws NullPointerException if the map is null.
     */
    public PassiveExpiringMap(final Map<K, V> map) {
        this(-1L, map);
    }

    /**
     * Normal {@link Map#clear()} behavior with the addition of clearing all
     * expiration entries as well.
     */
    @Override
    public void clear() {
        super.clear();
        expirationMap.clear();
    }

    /**
     * All expired entries are removed from the map prior to determining the
     * contains result.
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key) {
        removeIfExpired(key, now());
        return super.containsKey(key);
    }

    /**
     * All expired entries are removed from the map prior to determining the
     * contains result.
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value) {
        removeAllExpired(now());
        return super.containsValue(value);
    }

    /**
     * All expired entries are removed from the map prior to returning the entry set.
     * {@inheritDoc}
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        removeAllExpired(now());
        return map.entrySet();
    }

    /**
     * All expired entries are removed from the map prior to returning the entry value.
     * {@inheritDoc}
     */
    @Override
    public V get(final Object key) {
        removeIfExpired(key, now());
        return map.get(key);
    }

    /**
     * All expired entries are removed from the map prior to determining if it is empty.
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        removeAllExpired(now());
        return super.isEmpty();
    }

    /**
     * Determines if the given expiration time is less than {@code now}.
     *
     * @param now the time in milliseconds used to compare against the
     *        expiration time.
     * @param expirationTimeObject the expiration time value retrieved from
     *        {@link #expirationMap}, can be null.
     * @return {@code true} if {@code expirationTimeObject} is &ge; 0
     *         and {@code expirationTimeObject} &lt; {@code now}.
     *         {@code false} otherwise.
     */
    private boolean isExpired(final long now, final Long expirationTimeObject) {
        if (expirationTimeObject != null) {
            final long expirationTime = expirationTimeObject.longValue();
            return expirationTime >= 0 && now >= expirationTime;
        }
        return false;
    }

    /**
     * All expired entries are removed from the map prior to returning the key set.
     * {@inheritDoc}
     */
    @Override
    public Set<K> keySet() {
        removeAllExpired(now());
        return super.keySet();
    }

    /**
     * The current time in milliseconds.
     */
    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * Add the given key-value pair to this map as well as recording the entry's expiration time based on
     * the current time in milliseconds and this map's {@link #expiringPolicy}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public V put(final K key, final V value) {
        // remove the previous record
        removeIfExpired(key, now());

        // record expiration time of new entry
        final long expirationTime = expiringPolicy.expirationTime(key, value);
        expirationMap.put(key, Long.valueOf(expirationTime));

        return map.put(key, value);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> mapToCopy) {
        for (final Map.Entry<? extends K, ? extends V> entry : mapToCopy.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Normal {@link Map#remove(Object)} behavior with the addition of removing
     * any expiration entry as well.
     * {@inheritDoc}
     */
    @Override
    public V remove(final Object key) {
        expirationMap.remove(key);
        return super.remove(key);
    }

    /**
     * Removes all entries in the map whose expiration time is less than
     * {@code now}. The exceptions are entries with negative expiration
     * times; those entries are never removed.
     *
     * @see #isExpired(long, Long)
     */
    private void removeAllExpired(final long nowMillis) {
        final Iterator<Map.Entry<Object, Long>> iter = expirationMap.entrySet().iterator();
        while (iter.hasNext()) {
            final Map.Entry<Object, Long> expirationEntry = iter.next();
            if (isExpired(nowMillis, expirationEntry.getValue())) {
                // remove entry from collection
                map.remove(expirationEntry.getKey());
                // remove entry from expiration map
                iter.remove();
            }
        }
    }

    /**
     * Removes the entry with the given key if the entry's expiration time is
     * less than {@code now}. If the entry has a negative expiration time,
     * the entry is never removed.
     */
    private void removeIfExpired(final Object key, final long nowMillis) {
        final Long expirationTimeObject = expirationMap.get(key);
        if (isExpired(nowMillis, expirationTimeObject)) {
            remove(key);
        }
    }

    /**
     * All expired entries are removed from the map prior to returning the size.
     * {@inheritDoc}
     */
    @Override
    public int size() {
        removeAllExpired(now());
        return super.size();
    }

    /**
     * Read the map in using a custom routine.
     *
     * @param in the input stream
     * @throws IOException if an error occurs while reading from the stream
     * @throws ClassNotFoundException if an object read from the stream can not be loaded
     */
    @SuppressWarnings("unchecked")
    // (1) should only fail if input stream is incorrect
    private void readObject(final ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        map = (Map<K, V>) in.readObject(); // (1)
    }

    /**
     * Write the map out using a custom routine.
     *
     * @param out the output stream
     * @throws IOException if an error occurs while writing to the stream
     */
    private void writeObject(final ObjectOutputStream out)
            throws IOException {
        out.defaultWriteObject();
        out.writeObject(map);
    }

    /**
     * All expired entries are removed from the map prior to returning the value collection.
     * {@inheritDoc}
     */
    @Override
    public Collection<V> values() {
        removeAllExpired(now());
        return super.values();
    }
}
