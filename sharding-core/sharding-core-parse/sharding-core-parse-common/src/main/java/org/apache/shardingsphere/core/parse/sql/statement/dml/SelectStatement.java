/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.parse.sql.statement.dml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.shardingsphere.core.parse.sql.context.condition.ParseCondition;
import org.apache.shardingsphere.core.parse.sql.context.limit.Limit;
import org.apache.shardingsphere.core.parse.sql.context.orderby.OrderItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationDistinctSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.AggregationSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.DistinctSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.SelectItem;
import org.apache.shardingsphere.core.parse.sql.context.selectitem.StarSelectItem;
import org.apache.shardingsphere.core.parse.sql.context.table.Table;
import org.apache.shardingsphere.core.parse.util.SQLUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Select statement.
 *
 * @author zhangliang
 * @author panjuan
 */
@Getter
@Setter
@ToString(callSuper = true, exclude = "parentStatement")
public final class SelectStatement extends DQLStatement {
    
    private boolean containStar;
    
    private int firstSelectItemStartIndex;
    
    private int selectListStopIndex;
    
    private int groupByLastIndex;
    
    private final Set<SelectItem> items = new LinkedHashSet<>();
    
    private final List<OrderItem> groupByItems = new LinkedList<>();
    
    private final List<OrderItem> orderByItems = new LinkedList<>();
    
    private Limit limit;
    
    private SelectStatement parentStatement;
    
    private SelectStatement subqueryStatement;
    
    private Collection<ParseCondition> subqueryParseConditions = new LinkedList<>();
    
    /**
     * Get alias.
     * 
     * @param name name or alias
     * @return alias
     */
    public Optional<String> getAlias(final String name) {
        if (containStar) {
            return Optional.absent();
        }
        String rawName = SQLUtil.getExactlyValue(name);
        for (SelectItem each : items) {
            if (SQLUtil.getExactlyExpression(rawName).equalsIgnoreCase(SQLUtil.getExactlyExpression(SQLUtil.getExactlyValue(each.getExpression())))) {
                return each.getAlias();
            }
            if (rawName.equalsIgnoreCase(each.getAlias().orNull())) {
                return Optional.of(rawName);
            }
        }
        return Optional.absent();
    }
    
    /**
     * Get aggregation select items.
     *
     * @return aggregation select items
     */
    public List<AggregationSelectItem> getAggregationSelectItems() {
        List<AggregationSelectItem> result = new LinkedList<>();
        for (SelectItem each : items) {
            if (each instanceof AggregationSelectItem) {
                AggregationSelectItem aggregationSelectItem = (AggregationSelectItem) each;
                result.add(aggregationSelectItem);
                result.addAll(aggregationSelectItem.getDerivedAggregationSelectItems());
            }
        }
        return result;
    }
    
    /**
     * Get distinct select item optional.
     *
     * @return distinct select items
     */
    public Optional<DistinctSelectItem> getDistinctSelectItem() {
        for (SelectItem each : items) {
            if (each instanceof DistinctSelectItem) {
                return Optional.of((DistinctSelectItem) each);
            }
        }
        return Optional.absent();
    }
    
    /**
     * Get aggregation distinct select items.
     *
     * @return aggregation distinct select items
     */
    public List<AggregationDistinctSelectItem> getAggregationDistinctSelectItems() {
        List<AggregationDistinctSelectItem> result = new LinkedList<>();
        for (SelectItem each : items) {
            if (each instanceof AggregationDistinctSelectItem) {
                result.add((AggregationDistinctSelectItem) each);
            }
        }
        return result;
    }
    
    /**
     * Judge has unqualified star select item.
     * 
     * @return star select item without owner
     */
    public boolean hasUnqualifiedStarSelectItem() {
        for (SelectItem each : items) {
            if (each instanceof StarSelectItem && !((StarSelectItem) each).getOwner().isPresent()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get qualified star select items.
     *
     * @return qualified star select items
     */
    public Collection<StarSelectItem> getQualifiedStarSelectItems() {
        Collection<StarSelectItem> result = new LinkedList<>();
        for (SelectItem each : items) {
            if (each instanceof StarSelectItem && ((StarSelectItem) each).getOwner().isPresent()) {
                result.add((StarSelectItem) each);
            }
        }
        return result;
    }
    
    /**
     * Find star select item via table name or alias.
     *
     * @param tableNameOrAlias table name or alias
     * @return star select item via table name or alias
     */
    public Optional<StarSelectItem> findStarSelectItem(final String tableNameOrAlias) {
        Optional<Table> table = getTables().find(tableNameOrAlias);
        if (!table.isPresent()) {
            return Optional.absent();
        }
        for (SelectItem each : items) {
            if (!(each instanceof StarSelectItem)) {
                continue;
            }
            StarSelectItem starSelectItem = (StarSelectItem) each;
            if (starSelectItem.getOwner().isPresent() && getTables().find(starSelectItem.getOwner().get()).equals(table)) {
                return Optional.of(starSelectItem);
            }
        }
        return Optional.absent();
    }
    
    /**
     * Judge group by and order by sequence is same or not.
     *
     * @return group by and order by sequence is same or not
     */
    public boolean isSameGroupByAndOrderByItems() {
        return !getGroupByItems().isEmpty() && getGroupByItems().equals(getOrderByItems());
    }
    
    /**
     * Set index for select items.
     * 
     * @param columnLabelIndexMap map for column label and index
     */
    public void setIndexForItems(final Map<String, Integer> columnLabelIndexMap) {
        setIndexForAggregationItem(columnLabelIndexMap);
        setIndexForOrderItem(columnLabelIndexMap, orderByItems);
        setIndexForOrderItem(columnLabelIndexMap, groupByItems);
    }
    
    private void setIndexForAggregationItem(final Map<String, Integer> columnLabelIndexMap) {
        for (AggregationSelectItem each : getAggregationSelectItems()) {
            Preconditions.checkState(columnLabelIndexMap.containsKey(each.getColumnLabel()), "Can't find index: %s, please add alias for aggregate selections", each);
            each.setIndex(columnLabelIndexMap.get(each.getColumnLabel()));
            for (AggregationSelectItem derived : each.getDerivedAggregationSelectItems()) {
                Preconditions.checkState(columnLabelIndexMap.containsKey(derived.getColumnLabel()), "Can't find index: %s", derived);
                derived.setIndex(columnLabelIndexMap.get(derived.getColumnLabel()));
            }
        }
    }
    
    private void setIndexForOrderItem(final Map<String, Integer> columnLabelIndexMap, final List<OrderItem> orderItems) {
        for (OrderItem each : orderItems) {
            if (-1 != each.getIndex()) {
                continue;
            }
            Optional<String> alias = findAlias(each);
            String columnLabel = alias.isPresent() ? alias.get() : getOrderItemText(each);
            Preconditions.checkState(columnLabelIndexMap.containsKey(columnLabel), "Can't find index: %s", each);
            if (columnLabelIndexMap.containsKey(columnLabel)) {
                each.setIndex(columnLabelIndexMap.get(columnLabel));
            }
        }
    }
    
    private Optional<String> findAlias(final OrderItem orderItem) {
        Optional<String> result;
        if (orderItem.getQualifiedName().isPresent()) {
            result = getAlias(orderItem.getQualifiedName().get());
        } else if (null != orderItem.getExpression()) {
            result = getAlias(orderItem.getExpression());
        } else {
            result = Optional.absent();
        }
        return result;
    }
    
    private String getOrderItemText(final OrderItem orderItem) {
        if (orderItem.isIndex()) {
            return null;
        }
        if (orderItem.getName().isPresent()) {
            return orderItem.getName().get();
        }
        return orderItem.getExpression();
    }
}
