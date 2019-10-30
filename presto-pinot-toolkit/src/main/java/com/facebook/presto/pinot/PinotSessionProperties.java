/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.presto.pinot;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.session.PropertyMetadata;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import io.airlift.units.Duration;

import javax.inject.Inject;

import java.util.List;

import static com.facebook.presto.spi.session.PropertyMetadata.booleanProperty;
import static com.facebook.presto.spi.session.PropertyMetadata.integerProperty;
import static com.facebook.presto.spi.type.IntegerType.INTEGER;
import static com.facebook.presto.spi.type.VarcharType.createUnboundedVarcharType;
import static com.google.common.base.Preconditions.checkArgument;

public class PinotSessionProperties
{
    private static final String CONNECTION_TIMEOUT = "connection_timeout";
    private static final String PREFER_BROKER_QUERIES = "prefer_broker_queries";
    private static final String IGNORE_EMPTY_RESPONSES = "ignore_empty_responses";
    private static final String RETRY_COUNT = "retry_count";
    private static final String USE_DATE_TRUNC = "use_date_trunc";
    private static final String NON_AGGREGATE_LIMIT_FOR_BROKER_QUERIES = "non_aggregate_limit_for_broker_queries";

    @VisibleForTesting
    public static final String FORBID_SEGMENT_QUERIES = "forbid_segment_queries";

    @VisibleForTesting
    public static final String NUM_SEGMENTS_PER_SPLIT = "num_segments_per_split";

    private final List<PropertyMetadata<?>> sessionProperties;

    public static int getNumSegmentsPerSplit(ConnectorSession session)
    {
        int segmentsPerSplit = session.getProperty(NUM_SEGMENTS_PER_SPLIT, Integer.class);
        return segmentsPerSplit <= 0 ? Integer.MAX_VALUE : segmentsPerSplit;
    }

    public static boolean isPreferBrokerQueries(ConnectorSession session)
    {
        return session.getProperty(PREFER_BROKER_QUERIES, Boolean.class);
    }

    public static boolean isForbidSegmentQueries(ConnectorSession session)
    {
        return session.getProperty(FORBID_SEGMENT_QUERIES, Boolean.class);
    }

    public static Duration getConnectionTimeout(ConnectorSession session)
    {
        return session.getProperty(CONNECTION_TIMEOUT, Duration.class);
    }

    public static boolean isIgnoreEmptyResponses(ConnectorSession session)
    {
        return session.getProperty(IGNORE_EMPTY_RESPONSES, Boolean.class);
    }

    public static int getPinotRetryCount(ConnectorSession session)
    {
        return session.getProperty(RETRY_COUNT, Integer.class);
    }

    public static boolean isUseDateTruncation(ConnectorSession session)
    {
        return session.getProperty(USE_DATE_TRUNC, Boolean.class);
    }

    public static int getNonAggregateLimitForBrokerQueries(ConnectorSession session)
    {
        return session.getProperty(NON_AGGREGATE_LIMIT_FOR_BROKER_QUERIES, Integer.class);
    }

    @Inject
    public PinotSessionProperties(PinotConfig pinotConfig)
    {
        sessionProperties = ImmutableList.of(
                booleanProperty(
                        PREFER_BROKER_QUERIES,
                        "Prefer queries to broker even when parallel scan is enabled for aggregation queries",
                        pinotConfig.isPreferBrokerQueries(),
                        false),
                booleanProperty(
                        FORBID_SEGMENT_QUERIES,
                        "Forbid segment queries",
                        pinotConfig.isForbidSegmentQueries(),
                        false),
                booleanProperty(
                        IGNORE_EMPTY_RESPONSES,
                        "Ignore empty or missing pinot server responses",
                        pinotConfig.isIgnoreEmptyResponses(),
                        false),
                integerProperty(
                        RETRY_COUNT,
                        "Retry count for retriable pinot data fetch calls",
                        pinotConfig.getFetchRetryCount(),
                        false),
                integerProperty(
                        NON_AGGREGATE_LIMIT_FOR_BROKER_QUERIES,
                        "Max limit for non aggregate queries to the pinot broker",
                        pinotConfig.getNonAggregateLimitForBrokerQueries(),
                        false),
                booleanProperty(
                        USE_DATE_TRUNC,
                        "Use the new UDF dateTrunc in pinot that is more presto compatible",
                        pinotConfig.isUseDateTrunc(),
                        false),
                new PropertyMetadata<>(
                        CONNECTION_TIMEOUT,
                        "Connection Timeout to talk to Pinot servers",
                        createUnboundedVarcharType(),
                        Duration.class,
                        pinotConfig.getConnectionTimeout(),
                        false,
                        value -> Duration.valueOf((String) value),
                        Duration::toString),
                new PropertyMetadata<>(
                        NUM_SEGMENTS_PER_SPLIT,
                        "Number of segments of the same host per split",
                        INTEGER,
                        Integer.class,
                        pinotConfig.getNumSegmentsPerSplit(),
                        false,
                        value -> {
                            int ret = ((Number) value).intValue();
                            checkArgument(ret > 0, "Number of segments per split must be more than zero");
                            return ret;
                        },
                        object -> object));
    }

    public List<PropertyMetadata<?>> getSessionProperties()
    {
        return sessionProperties;
    }
}