package predcompiler.compilation.grammarevolution

import org.apache.commons.math3.random.MersenneTwister
import java.security.SecureRandom
import java.util.*

class SynchronizedMersenneTwister private constructor() : Random() {
    private fun current(): MersenneTwister {
        return LOCAL_RANDOM.get()
    }

    @Synchronized
    override fun setSeed(seed: Long) {
        current().setSeed(seed)
    }

    override fun nextBytes(bytes: ByteArray) {
        current().nextBytes(bytes)
    }

    override fun nextInt(): Int {
        return current().nextInt()
    }

    override fun nextInt(n: Int): Int {
        //return Math.max(current().nextInt(n), 1);
        return current().nextInt(n)
    }

    override fun nextLong(): Long {
        return current().nextLong()
    }

    override fun nextBoolean(): Boolean {
        return current().nextBoolean()
    }

    override fun nextFloat(): Float {
        return current().nextFloat()
    }

    override fun nextDouble(): Double {
        return current().nextDouble()
    }

    @Synchronized
    override fun nextGaussian(): Double {
        return current().nextGaussian()
    }

    companion object {

        private var SEEDER: Random = SecureRandom()

        @JvmStatic
        val instance: SynchronizedMersenneTwister = SynchronizedMersenneTwister()

        private var LOCAL_RANDOM: ThreadLocal<MersenneTwister> = object : ThreadLocal<MersenneTwister>() {
            override fun initialValue(): MersenneTwister {
                synchronized(SEEDER) {
                    return MersenneTwister(SEEDER.nextLong())
                }
            }
        }
    }
}
