package com.seunome.rifly

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val SUPABASE_URL = "https://seiewvnsnbzmvjlgqrjr.supabase.co"
    const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNlaWV3dm5zbmJ6bXZqbGdxcmpyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA5NzczNTcsImV4cCI6MjEwNjU1MzM1N30.kGubqqpB_0ZQevyT0S9QhaKcH_QTNgA-N_nulOdo930"

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
