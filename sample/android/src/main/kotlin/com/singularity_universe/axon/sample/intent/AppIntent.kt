package com.singularity_universe.axon.sample.intent

import com.singularity_universe.axon.Intent

/**
 * Root intent representing the application being open.
 *
 * Created once at app start and passed as [parent] to all top-level operations.
 * Not dispatched through Axon — exists solely to anchor the intent chain.
 */
class AppIntent(parent: Intent<*>?) : Intent<Unit>(parent)
