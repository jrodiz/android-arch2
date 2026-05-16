package com.rodiz.arch2.feature.home.data

import com.rodiz.arch2.feature.home.domain.model.Author
import com.rodiz.arch2.feature.home.domain.model.AuthorId
import com.rodiz.arch2.feature.home.domain.model.Post
import com.rodiz.arch2.feature.home.domain.model.PostId
import com.rodiz.arch2.feature.home.domain.model.Reactions
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val now get() = Clock.System.now()

private val mayaPark = Author(
    id = AuthorId("u_maya"),
    displayName = "Maya Park",
    avatarUrl = "https://i.pravatar.cc/150?img=47",
)
private val jordanLee = Author(
    id = AuthorId("u_jordan"),
    displayName = "Jordan Lee",
    avatarUrl = "https://i.pravatar.cc/150?img=12",
)
private val sashaKim = Author(
    id = AuthorId("u_sasha"),
    displayName = "Sasha Kim",
    avatarUrl = "https://i.pravatar.cc/150?img=32",
)
private val rileyChen = Author(
    id = AuthorId("u_riley"),
    displayName = "Riley Chen",
    avatarUrl = null,
)
private val priyaShah = Author(
    id = AuthorId("u_priya"),
    displayName = "Priya Shah",
    avatarUrl = "https://i.pravatar.cc/150?img=20",
)
private val theoMartinez = Author(
    id = AuthorId("u_theo"),
    displayName = "Theo Martinez",
    avatarUrl = "https://i.pravatar.cc/150?img=68",
)

internal val FakeFeedSeed: List<Post>
    get() = listOf(
        Post(
            id = PostId("p_001"),
            author = mayaPark,
            createdAt = now - 4.minutes,
            text = "Beach day with Mochi! He's officially a swimmer now. 🏖️",
            imageUrl = "https://picsum.photos/seed/mochi-beach/800/600",
            reactions = Reactions(likeCount = 142, loveCount = 38, hahaCount = 4, wowCount = 6, sadCount = 0, angryCount = 0),
            commentCount = 23,
            shareCount = 5,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_002"),
            author = jordanLee,
            createdAt = now - 28.minutes,
            text = "Hot take: cold brew is just coffee that gave up.",
            imageUrl = null,
            reactions = Reactions(likeCount = 87, loveCount = 2, hahaCount = 54, wowCount = 1, sadCount = 0, angryCount = 12),
            commentCount = 41,
            shareCount = 3,
            viewerHasLiked = true,
        ),
        Post(
            id = PostId("p_003"),
            author = sashaKim,
            createdAt = now - 2.hours,
            text = null,
            imageUrl = "https://picsum.photos/seed/sunset-bridge/800/600",
            reactions = Reactions(likeCount = 312, loveCount = 95, hahaCount = 1, wowCount = 27, sadCount = 0, angryCount = 0),
            commentCount = 18,
            shareCount = 9,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_004"),
            author = priyaShah,
            createdAt = now - 3.hours,
            text = "Just finished my first half marathon 🏃‍♀️ Six months of training, totally worth it. " +
                "Thanks to everyone who showed up at mile 9 with signs.",
            imageUrl = "https://picsum.photos/seed/halfmarathon/800/600",
            reactions = Reactions(likeCount = 524, loveCount = 201, hahaCount = 0, wowCount = 33, sadCount = 0, angryCount = 0),
            commentCount = 87,
            shareCount = 4,
            viewerHasLiked = true,
        ),
        Post(
            id = PostId("p_005"),
            author = rileyChen,
            createdAt = now - 5.hours,
            text = "If anyone in the neighborhood is missing an orange tabby, he has been hanging out on my porch for two days and is very chatty.",
            imageUrl = null,
            reactions = Reactions(likeCount = 64, loveCount = 11, hahaCount = 8, wowCount = 4, sadCount = 0, angryCount = 0),
            commentCount = 29,
            shareCount = 14,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_006"),
            author = theoMartinez,
            createdAt = now - 7.hours,
            text = "New album dropped at midnight. Track 4 is the one. 🎷",
            imageUrl = "https://picsum.photos/seed/vinyl-record/800/600",
            reactions = Reactions(likeCount = 199, loveCount = 76, hahaCount = 0, wowCount = 12, sadCount = 0, angryCount = 0),
            commentCount = 35,
            shareCount = 22,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_007"),
            author = mayaPark,
            createdAt = now - 11.hours,
            text = "Tried a new ramen place near the office. 9/10. Would have been 10 but they were out of ajitama.",
            imageUrl = "https://picsum.photos/seed/ramen-bowl/800/600",
            reactions = Reactions(likeCount = 88, loveCount = 14, hahaCount = 2, wowCount = 0, sadCount = 3, angryCount = 0),
            commentCount = 12,
            shareCount = 1,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_008"),
            author = jordanLee,
            createdAt = now - 14.hours,
            text = "Six hours debugging a feature flag that was on the whole time.",
            imageUrl = null,
            reactions = Reactions(likeCount = 421, loveCount = 12, hahaCount = 380, wowCount = 5, sadCount = 19, angryCount = 0),
            commentCount = 102,
            shareCount = 47,
            viewerHasLiked = true,
        ),
        Post(
            id = PostId("p_009"),
            author = sashaKim,
            createdAt = now - 1.days,
            text = "Throwback to last summer's road trip. We had no plan and somehow ended up in three states.",
            imageUrl = "https://picsum.photos/seed/road-trip/800/600",
            reactions = Reactions(likeCount = 156, loveCount = 48, hahaCount = 7, wowCount = 9, sadCount = 0, angryCount = 0),
            commentCount = 19,
            shareCount = 2,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_010"),
            author = priyaShah,
            createdAt = now - 1.days - 6.hours,
            text = null,
            imageUrl = "https://picsum.photos/seed/latte-art/800/600",
            reactions = Reactions(likeCount = 73, loveCount = 21, hahaCount = 1, wowCount = 14, sadCount = 0, angryCount = 0),
            commentCount = 8,
            shareCount = 0,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_011"),
            author = rileyChen,
            createdAt = now - 2.days,
            text = "Garden update: tomatoes are finally taking off. Basil is on its third life.",
            imageUrl = "https://picsum.photos/seed/garden/800/600",
            reactions = Reactions(likeCount = 49, loveCount = 17, hahaCount = 4, wowCount = 2, sadCount = 0, angryCount = 0),
            commentCount = 11,
            shareCount = 0,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_012"),
            author = theoMartinez,
            createdAt = now - 2.days - 4.hours,
            text = "Open mic tonight at The Rusty Tap, 9pm. First round is on me if you make it out.",
            imageUrl = null,
            reactions = Reactions(likeCount = 38, loveCount = 9, hahaCount = 0, wowCount = 0, sadCount = 1, angryCount = 0),
            commentCount = 6,
            shareCount = 8,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_013"),
            author = mayaPark,
            createdAt = now - 3.days,
            text = "Finally finished reading East of Eden. It took six months and I have a lot of feelings.",
            imageUrl = "https://picsum.photos/seed/east-of-eden/800/600",
            reactions = Reactions(likeCount = 112, loveCount = 27, hahaCount = 0, wowCount = 8, sadCount = 3, angryCount = 0),
            commentCount = 24,
            shareCount = 3,
            viewerHasLiked = true,
        ),
        Post(
            id = PostId("p_014"),
            author = jordanLee,
            createdAt = now - 4.days,
            text = "Anyone else's cat refuse to drink water from the bowl but happily lick it off the floor? Asking for me.",
            imageUrl = null,
            reactions = Reactions(likeCount = 201, loveCount = 18, hahaCount = 156, wowCount = 11, sadCount = 0, angryCount = 0),
            commentCount = 67,
            shareCount = 14,
            viewerHasLiked = false,
        ),
        Post(
            id = PostId("p_015"),
            author = sashaKim,
            createdAt = now - 5.days,
            text = "First print of the year is finally framed and on the wall. Took longer to pick the frame than to shoot it.",
            imageUrl = "https://picsum.photos/seed/framed-print/800/600",
            reactions = Reactions(likeCount = 94, loveCount = 31, hahaCount = 1, wowCount = 18, sadCount = 0, angryCount = 0),
            commentCount = 15,
            shareCount = 1,
            viewerHasLiked = false,
        ),
    )

internal val FakeRefreshAuthors: List<Author> = listOf(mayaPark, jordanLee, sashaKim, priyaShah, theoMartinez, rileyChen)

internal val FakeRefreshSnippets: List<Pair<String?, String?>> = listOf(
    "Coffee → code → repeat. Morning routine locked in." to "https://picsum.photos/seed/refresh-coffee/800/600",
    "Just discovered the best taco truck in the city. DM me." to null,
    null to "https://picsum.photos/seed/refresh-skyline/800/600",
    "Quick sketch from the park bench today." to "https://picsum.photos/seed/refresh-sketch/800/600",
    "Today's mood: a single deploy with no rollbacks." to null,
)
