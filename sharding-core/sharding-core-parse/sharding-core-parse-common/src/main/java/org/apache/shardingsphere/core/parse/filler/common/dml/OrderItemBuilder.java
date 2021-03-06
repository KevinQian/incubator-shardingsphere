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

package org.apache.shardingsphere.core.parse.filler.common.dml;

import com.google.common.base.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.core.parse.sql.context.orderby.OrderItem;
import org.apache.shardingsphere.core.parse.sql.segment.common.TableSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.ExpressionOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.OrderByItemSegment;

/**
 * Order item builder.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class OrderItemBuilder {
    
    private final OrderByItemSegment orderByItemSegment;
    
    /**
     * Create order item.
     * 
     * @return order item
     */
    public OrderItem createOrderItem() {
        if (orderByItemSegment instanceof IndexOrderByItemSegment) {
            return createOrderItem((IndexOrderByItemSegment) orderByItemSegment);
        }
        if (orderByItemSegment instanceof ColumnOrderByItemSegment) {
            return createOrderItem((ColumnOrderByItemSegment) orderByItemSegment);
        }
        if (orderByItemSegment instanceof ExpressionOrderByItemSegment) {
            return createOrderItem((ExpressionOrderByItemSegment) orderByItemSegment);
        }
        throw new UnsupportedOperationException();
    }
    
    private OrderItem createOrderItem(final IndexOrderByItemSegment indexOrderByItemSegment) {
        return new OrderItem(indexOrderByItemSegment.getColumnIndex(), indexOrderByItemSegment.getOrderDirection(), indexOrderByItemSegment.getNullOrderDirection());
    }
    
    private OrderItem createOrderItem(final ColumnOrderByItemSegment columnOrderByItemSegment) {
        Optional<TableSegment> owner = columnOrderByItemSegment.getColumn().getOwner();
        String columnName = columnOrderByItemSegment.getColumn().getName();
        return owner.isPresent() ? new OrderItem(owner.get().getName(), columnName, columnOrderByItemSegment.getOrderDirection(), columnOrderByItemSegment.getNullOrderDirection())
                : new OrderItem(columnName, columnOrderByItemSegment.getOrderDirection(), columnOrderByItemSegment.getNullOrderDirection());
    }
    
    private OrderItem createOrderItem(final ExpressionOrderByItemSegment expressionOrderByItemSegment) {
        return new OrderItem(expressionOrderByItemSegment.getExpression(), expressionOrderByItemSegment.getOrderDirection(), expressionOrderByItemSegment.getNullOrderDirection());
    }
}
