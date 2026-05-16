package com.rodiz.arch2.feature.home.data

import com.rodiz.arch2.feature.home.domain.model.Post
import com.rodiz.arch2.feature.home.domain.model.PostId
import com.rodiz.arch2.feature.home.domain.model.Reactions
import com.rodiz.arch2.feature.home.domain.repository.PostRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakePostRepository @Inject constructor() : PostRepository {

    private val state = MutableStateFlow(FakeFeedSeed)
    private val refreshMutex = Mutex()

    override fun observeFeed(): Flow<List<Post>> = state.asStateFlow()

    override suspend fun refresh() {
        refreshMutex.withLock {
            delay(Random.nextLong(600, 1200))
            if (Random.nextFloat() < FAILURE_RATE) {
                throw IOException("Simulated network failure")
            }
            val freshCount = Random.nextInt(1, 4)
            val fresh = List(freshCount) { buildFreshPost() }
            state.update { fresh + it }
        }
    }

    override suspend fun toggleLike(postId: PostId) {
        state.update { posts ->
            posts.map { post ->
                if (post.id != postId) return@map post
                val newlyLiked = !post.viewerHasLiked
                val delta = if (newlyLiked) 1 else -1
                post.copy(
                    viewerHasLiked = newlyLiked,
                    reactions = post.reactions.copy(
                        likeCount = (post.reactions.likeCount + delta).coerceAtLeast(0),
                    ),
                )
            }
        }
    }

    private fun buildFreshPost(): Post {
        val (text, imageUrl) = FakeRefreshSnippets.random()
        val author = FakeRefreshAuthors.random()
        return Post(
            id = PostId("p_${UUID.randomUUID().toString().take(8)}"),
            author = author,
            createdAt = Clock.System.now(),
            text = text,
            imageUrl = imageUrl,
            reactions = Reactions.Empty.copy(likeCount = Random.nextInt(0, 12)),
            commentCount = Random.nextInt(0, 5),
            shareCount = Random.nextInt(0, 3),
            viewerHasLiked = false,
        )
    }

    private companion object {
        const val FAILURE_RATE = 0.1f
    }
}
