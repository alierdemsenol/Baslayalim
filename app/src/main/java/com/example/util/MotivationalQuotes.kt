package com.example.util

data class Quote(
    val text: String,
    val author: String
)

private data class BilingualQuote(
    val trText: String,
    val enText: String,
    val trAuthor: String,
    val enAuthor: String
)

object MotivationalQuotes {

    private val bilingualQuotes = listOf(
        BilingualQuote(
            "Zafer, 'Zafer benimdir' diyebilenindir. Başarı ise 'Başaracağım' diye başlayarak sonunda 'Başardım' diyebilenindir.",
            "Victory belongs to those who can say 'Victory is mine.' Success belongs to those who start with 'I will succeed' and end with 'I succeeded.'",
            "Mustafa Kemal Atatürk", "Mustafa Kemal Atatürk"
        ),
        BilingualQuote(
            "Hayatınız, düşüncelerinizin eseridir.",
            "Your life is what your thoughts make it.",
            "Marcus Aurelius", "Marcus Aurelius"
        ),
        BilingualQuote(
            "Öğrenmeyi bıraktığın gün ölmeye başladığın gündür.",
            "Once you stop learning, you start dying.",
            "Albert Einstein", "Albert Einstein"
        ),
        BilingualQuote(
            "Zorluklar, karakteri çelikleştirir.",
            "Difficulties strengthen the mind, as labor does the body.",
            "Seneca", "Seneca"
        ),
        BilingualQuote(
            "Gelecek, bugünden hazırlananlara aittir.",
            "The future belongs to those who prepare for it today.",
            "Malcolm X", "Malcolm X"
        ),
        BilingualQuote(
            "Hayatta en hakiki mürşit ilimdir, fendir.",
            "The truest guide in life is science and knowledge.",
            "Mustafa Kemal Atatürk", "Mustafa Kemal Atatürk"
        ),
        BilingualQuote(
            "Başarı, her gün tekrarlanan küçük çabaların toplamıdır.",
            "Success is the sum of small efforts, repeated day in and day out.",
            "Robert Collier", "Robert Collier"
        ),
        BilingualQuote(
            "Gideceği limanı bilmeyene hiçbir rüzgardan fayda gelmez.",
            "No wind favors a ship that has no destination port.",
            "Montaigne", "Montaigne"
        ),
        BilingualQuote(
            "Işığa ulaşmak için karanlığa göğüs germek gerekir.",
            "Even the darkest night will end and the sun will rise.",
            "Victor Hugo", "Victor Hugo"
        ),
        BilingualQuote(
            "Sadece çok uzağa gitme riskini göze alanlar ne kadar uzağa gidebileceklerini öğrenirler.",
            "Only those who risk going too far can possibly find out how far one can go.",
            "T.S. Eliot", "T.S. Eliot"
        ),
        BilingualQuote(
            "Disiplin, hedefleriniz ile başarı arasındaki köprüdür.",
            "Discipline is the bridge between goals and accomplishment.",
            "Jim Rohn", "Jim Rohn"
        ),
        BilingualQuote(
            "En büyük zafer, hiç düşmemek değil, her düştüğünde ayağa kalkmaktır.",
            "Our greatest glory is not in never falling, but in rising every time we fall.",
            "Konfüçyüs", "Confucius"
        ),
        BilingualQuote(
            "Büyük işler başarmak için sadece harekete geçmek yetmez, hayal de etmeliyiz.",
            "To accomplish great things, we must not only act, but also dream.",
            "Anatole France", "Anatole France"
        ),
        BilingualQuote(
            "Bilgi size güç verir; ama karakter saygı kazandırır.",
            "Knowledge will give you power, but character respect.",
            "Bruce Lee", "Bruce Lee"
        ),
        BilingualQuote(
            "Başarısızlık, daha zekice başlama fırsatından başka bir şey değildir.",
            "Failure is simply the opportunity to begin again, this time more intelligently.",
            "Henry Ford", "Henry Ford"
        ),
        BilingualQuote(
            "Geleceği tahmin etmenin en iyi yolu, onu yaratmaktır.",
            "The best way to predict the future is to create it.",
            "Peter Drucker", "Peter Drucker"
        ),
        BilingualQuote(
            "Zaman en değerli hazinedir, onu hayallerin için harca.",
            "Time is the most valuable treasure; spend it on your dreams.",
            "Sokrates", "Socrates"
        ),
        BilingualQuote(
            "Başlamak için harika olmak zorunda değilsin, ama harika olmak için başlamak zorundasın.",
            "You don't have to be great to start, but you have to start to be great.",
            "Zig Ziglar", "Zig Ziglar"
        ),
        BilingualQuote(
            "Sınırlarını zorlamayan insan, asla ne kadar güçlü olduğunu bilemez.",
            "One who never tests their limits can never know how strong they truly are.",
            "Mevlana", "Rumi"
        ),
        BilingualQuote(
            "Engeller, gözünüzü hedeften ayırdığınızda gördüğünüz korkunç şeylerdir.",
            "Obstacles are those frightful things you see when you take your eyes off your goal.",
            "Henry Ford", "Henry Ford"
        ),
        BilingualQuote(
            "Muhtaç olduğun kudret, damarlarındaki asil kanda mevcuttur!",
            "The strength you need is already within you!",
            "Mustafa Kemal Atatürk", "Mustafa Kemal Atatürk"
        ),
        BilingualQuote(
            "Bugün çektiğin her zorluk, yarın anlatacağın başarı hikayenin bir parçasıdır.",
            "Every struggle today becomes part of the success story you tell tomorrow.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Sessizce ve kararlılıkla çalış; bırak başarın gürültü çıkarsın.",
            "Work hard in silence; let your success be your noise.",
            "Frank Ocean", "Frank Ocean"
        ),
        BilingualQuote(
            "Kaliteniz, kimsenin bakmadığı anlarda yaptıklarınızla ölçülür.",
            "Quality means doing it right when no one is looking.",
            "Henry Ford", "Henry Ford"
        ),
        BilingualQuote(
            "Ulaşmak istediğin hedef, vazgeçmediğin sürece sana her gün bir adım daha yaklaşır.",
            "Your goal draws one step closer every day as long as you never give up.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Kendine inan; çünkü sen inandığında başarmak kaçınılmaz olur.",
            "Believe in yourself; when you truly believe, success becomes inevitable.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Büyük hayaller kurun ve başarısız olmayı göze alın.",
            "Dream big and dare to fail.",
            "Norman Vaughan", "Norman Vaughan"
        ),
        BilingualQuote(
            "Mükemmellik bir eylem değil, bir alışkanlıktır.",
            "Excellence is not an act, but a habit.",
            "Aristoteles", "Aristotle"
        ),
        BilingualQuote(
            "Eylemsizlik şüphe ve korku doğurur. Eylem ise güven ve cesaret.",
            "Inaction breeds doubt and fear. Action breeds confidence and courage.",
            "Dale Carnegie", "Dale Carnegie"
        ),
        BilingualQuote(
            "Yorulduğunda durma; hedefine ulaştığında dinlen.",
            "Do not stop when you are tired; rest when you reach your goal.",
            "Mete Gazoz", "Mete Gazoz"
        ),
        BilingualQuote(
            "Şans, hazırlıklı zihinlere güler.",
            "Chance favors only the prepared mind.",
            "Louis Pasteur", "Louis Pasteur"
        ),
        BilingualQuote(
            "Işık saçmak için yanmak gerekir.",
            "To shine light, one must endure the fire.",
            "Mevlana", "Rumi"
        ),
        BilingualQuote(
            "Masadaki her saat, gelecekteki özgürlüğüne atılan bir adımdır.",
            "Every hour at your desk is a step toward your future freedom.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Başarısızlık yok, sadece geri bildirim ve gelişim var.",
            "There is no failure, only feedback and growth.",
            "Tony Robbins", "Tony Robbins"
        ),
        BilingualQuote(
            "Yapabileceğinize inansanız da inanmasanız da haklısınız.",
            "Whether you think you can, or you think you can't, you're right.",
            "Henry Ford", "Henry Ford"
        ),
        BilingualQuote(
            "Dünyayı değiştirecek olanlar, onu değiştirebileceklerini düşünecek kadar cesur olanlardır.",
            "The people who are crazy enough to think they can change the world are the ones who do.",
            "Steve Jobs", "Steve Jobs"
        ),
        BilingualQuote(
            "Rakiplerin uyurken senin masada olman, farkı yaratan şeydir.",
            "Being at your desk while others sleep is what makes all the difference.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Başarı bir yolculuktur, bir varış noktası değil.",
            "Success is a journey, not a destination.",
            "Ben Sweetland", "Ben Sweetland"
        ),
        BilingualQuote(
            "Sabır ve zaman, güç ve öfkeden daha çok şey başarır.",
            "Patience and time do more than strength or passion.",
            "Jean de La Fontaine", "Jean de La Fontaine"
        ),
        BilingualQuote(
            "Kimse geriye gidip yeni bir başlangıç yapamaz ama bugün başlayıp yeni bir son yazabilir.",
            "Though no one can go back and make a brand new start, anyone can start from now and make a brand new ending.",
            "Carl Bard", "Carl Bard"
        ),
        BilingualQuote(
            "Zihnin sınırı neredeyse, başarının sınırı da oradadır.",
            "Where the mind goes, the limits of success follow.",
            "Arnold Schwarzenegger", "Arnold Schwarzenegger"
        ),
        BilingualQuote(
            "Zorluklar başarının lezzetini artıran baharatlardır.",
            "Difficulties are the spice that gives flavor to success.",
            "Truman Capote", "Truman Capote"
        ),
        BilingualQuote(
            "Hedefine ulaşmak için attığın her adım, dünkünden daha güçlü olmanı sağlar.",
            "Every step toward your goal makes you stronger than you were yesterday.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Sorularından korkma, yapamadığın her soru öğrenme fırsatıdır.",
            "Never fear tough questions; every missed question is a chance to learn.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Akıl ve azim birleştiğinde aşılmayacak engel yoktur.",
            "When mind and determination unite, no obstacle is insurmountable.",
            "Sokrates", "Socrates"
        ),
        BilingualQuote(
            "Hayalleriniz hedefiniz olsun, bahaneleriniz değil.",
            "Make your dreams your goal, not your excuses.",
            "Başlayalım Rehberlik", "Başlayalım Guidance"
        ),
        BilingualQuote(
            "Zamanını yöneten, geleceğini yönetir.",
            "Those who manage their time manage their future.",
            "Peter Drucker", "Peter Drucker"
        ),
        BilingualQuote(
            "Başarılı insanlar pes etmeyen insanlardır.",
            "Success is never final; it is the courage to continue that counts.",
            "Winston Churchill", "Winston Churchill"
        )
    )

    val quotes: List<Quote> = bilingualQuotes.map { Quote(it.trText, it.trAuthor) }

    fun getQuoteForUsageDay(daysCount: Int, lang: String = "TR"): Quote {
        val index = (daysCount - 1).coerceAtLeast(0) % bilingualQuotes.size
        val item = bilingualQuotes[index]
        return if (lang.equals("TR", ignoreCase = true)) {
            Quote(item.trText, item.trAuthor)
        } else {
            Quote(item.enText, item.enAuthor)
        }
    }
}
