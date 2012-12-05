package com.facebook.presto.benchmark;

import com.facebook.presto.block.BlockCursor;
import com.facebook.presto.block.BlockIterable;
import com.facebook.presto.operator.AlignmentOperator;
import com.facebook.presto.operator.HashJoinOperator;
import com.facebook.presto.operator.Operator;
import com.facebook.presto.operator.Page;
import com.facebook.presto.operator.SourceHashProvider;
import com.facebook.presto.serde.BlocksFileEncoding;
import com.facebook.presto.tpch.TpchBlocksProvider;

public class HashJoinBenchmark
        extends AbstractOperatorBenchmark
{
    private SourceHashProvider sourceHashProvider;

    public HashJoinBenchmark()
    {
        super("hash_join", 4, 5);
    }

    /*
    select orderkey, quantity, totalprice
    from lineitem join orders using (orderkey)
     */
    @Override
    protected Operator createBenchmarkedOperator(TpchBlocksProvider blocksProvider)
    {
        if (sourceHashProvider == null) {
            BlockIterable orderOrderKey = getBlockIterable(blocksProvider, "orders", "orderkey", BlocksFileEncoding.RAW);
            BlockIterable totalPrice = getBlockIterable(blocksProvider, "orders", "totalprice", BlocksFileEncoding.RAW);
            AlignmentOperator ordersTableScan = new AlignmentOperator(orderOrderKey, totalPrice);
//            AlignmentOperator ordersTableScan = new AlignmentOperator(concat(nCopies(100, orderOrderKey)), concat(nCopies(100, totalPrice)));
//            LimitOperator ordersLimit = new LimitOperator(ordersTableScan, 1_500_000);
            sourceHashProvider = new SourceHashProvider(ordersTableScan, 0, 1_500_000);
        }

        BlockIterable lineItemOrderKey = getBlockIterable(blocksProvider, "lineitem", "orderkey", BlocksFileEncoding.RAW);
        BlockIterable lineNumber = getBlockIterable(blocksProvider, "lineitem", "quantity", BlocksFileEncoding.RAW);
        AlignmentOperator lineItemTableScan = new AlignmentOperator(lineItemOrderKey, lineNumber);
//        AlignmentOperator lineItemTableScan = new AlignmentOperator(concat(nCopies(100, lineItemOrderKey)), concat(nCopies(100, lineNumber)));
//        LimitOperator lineItemLimit = new LimitOperator(lineItemTableScan, 10_000_000);

        return new HashJoinOperator(sourceHashProvider, lineItemTableScan, 0);
    }

    @Override
    protected long execute(TpchBlocksProvider blocksProvider)
    {
        Operator operator = createBenchmarkedOperator(blocksProvider);

        long outputRows = 0;
        for (Page page : operator) {
            BlockCursor cursor = page.getBlock(0).cursor();
            while (cursor.advanceNextPosition()) {
                outputRows++;
            }
        }
        return outputRows;
    }

    public static void main(String[] args)
    {
        new HashJoinBenchmark().runBenchmark(
                new SimpleLineBenchmarkResultWriter(System.out)
        );
    }
}