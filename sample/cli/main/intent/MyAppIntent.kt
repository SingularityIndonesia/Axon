package intent

import com.singularity_universe.axon.Intent

sealed class MyAppIntent<out R>(parent: Intent<*>? = null) : Intent<R>(parent)
