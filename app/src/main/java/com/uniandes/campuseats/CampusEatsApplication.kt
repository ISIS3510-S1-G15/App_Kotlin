package com.uniandes.campuseats

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageResult
import kotlin.concurrent.thread

/**
 * - Arranca [AppServices] en un hilo de fondo (crear el Keystore / Room / DataStore en el hilo principal
 *   congelaba el primer frame).
 * - Configura el cache de imagenes de Coil (memoria + disco) segun las preferencias del usuario.
 */
class CampusEatsApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        thread(name = "services-warmup") { runCatching { AppServices.get(this) } }
    }

    override fun newImageLoader(): ImageLoader {
        val services = AppServices.get(this)
        val prefs = services.preferences
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(prefs.current.value.memoryCacheMb * 1024 * 1024)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(IMAGE_DISK_BYTES)
                    .build()
            }
            .components {
                // Ahorro de datos: con datos moviles solo se muestran imagenes que ya estan en cache
                add(object : Interceptor {
                    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
                        val saver = prefs.current.value.dataSaver && services.connectivity.metered.value
                        val request = if (saver) {
                            chain.request.newBuilder().networkCachePolicy(CachePolicy.DISABLED).build()
                        } else {
                            chain.request
                        }
                        return chain.proceed(request)
                    }
                })
            }
            .crossfade(true)
            .build()
    }

    private companion object {
        const val IMAGE_DISK_BYTES = 64L * 1024 * 1024
    }
}
