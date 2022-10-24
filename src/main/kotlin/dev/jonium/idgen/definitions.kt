package dev.jonium.idgen

/*
In total 64 bits are divided as follows:
42 bits TIME - Approx 100 yrs.
5  bits THR  - Thread
4  bits RND  - Other
13 bits SEQ  - Sequence
 */

/*
Epoch is defined to be at 2021-01-01 00:00
*/

internal const val EPOCH: Long = 1609459200000L

internal const val SHL_RND: Int = 13
internal const val SHL_THR: Int = 17
internal const val SHL_TIME: Int = 22

internal const val MASK_RND: Long = 7
internal const val MASK_THR: Long = 31
internal const val MASK_SEQ: Long = 8191

internal const val MOD_THR: Long = MASK_THR + 1
internal const val MOD_RND: Long = MASK_RND + 1
internal const val MOD_SEQ: Long = MASK_SEQ + 1
