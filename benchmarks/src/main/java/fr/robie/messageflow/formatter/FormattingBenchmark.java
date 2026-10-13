package fr.robie.messageflow.formatter;

import fr.robie.messageflow.TextResolverRegistry;
import fr.robie.messageflow.api.GlobalPlaceholderRegistry;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The formatting hot paths. Run with {@code java -jar benchmarks/target/benchmarks.jar}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class FormattingBenchmark {
    private static final String STATIC = "<gray>[<gold>Shop</gold>] <white>Welcome back to the shop!";
    private static final String SHOP = "<gray>[<gold>Shop</gold>] <white>You bought <yellow>%amount%x %item%</yellow> for "
            + "<green>%price% coins</green>! <click:run_command:'/shop'><u>Open shop</u></click>";
    private static final String MODIFIERS = "<gold>%coins.comma% coins, %count% %count.plural:item:items%";
    private static final String GLOBAL = "<gray>Welcome to %server%, %online% players online";
    private static final String LEGACY = "&6[Shop] &7You bought &e%amount%x %item% &7for #ffaa00%price% coins &a&l!";

    private AdventureMessageFormatter<Plugin> adventure;
    private LegacyMessageFormatter<Plugin> legacy;
    private Placeholder shop;
    private Placeholder modifiers;
    private List<Player> players;

    @Setup
    public void setUp() {
        Plugin plugin = fake(Plugin.class);
        TextResolverRegistry registry = new TextResolverRegistry();
        registry.register(new FunctionalPlaceholderResolver());

        this.adventure = new AdventureMessageFormatter<>(plugin);
        this.adventure.setTextResolverRegistry(registry);
        this.legacy = new LegacyMessageFormatter<>(plugin);
        this.legacy.setTextResolverRegistry(registry);

        GlobalPlaceholderRegistry.getInstance().register("server", "Lobby");
        GlobalPlaceholderRegistry.getInstance().register("online", "42");

        this.shop = Placeholder.builder().register("amount", "3").register("item", "Diamond").register("price", "120").build();
        this.modifiers = Placeholder.of("coins", "1234567", "count", "3");
        this.players = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            this.players.add(fake(Player.class));
        }
    }

    @TearDown
    public void tearDown() {
        GlobalPlaceholderRegistry.getInstance().clear();
    }

    /**
     * An interface whose methods do nothing and return a default value.
     */
    @SuppressWarnings("unchecked")
    private static <I> I fake(Class<I> type) {
        return (I) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Class<?> returned = method.getReturnType();
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            if (returned == boolean.class) {
                return false;
            }
            if (returned.isPrimitive() && returned != void.class) {
                return returned == long.class ? 0L : returned == double.class ? 0d : returned == float.class ? 0f : 0;
            }
            return null;
        });
    }

    @Benchmark
    public Component staticMessage() {
        return this.adventure.format(STATIC, null, Placeholder.empty());
    }

    @Benchmark
    public Component localPlaceholders() {
        return this.adventure.format(SHOP, null, this.shop);
    }

    @Benchmark
    public Component modifiers() {
        return this.adventure.format(MODIFIERS, null, this.modifiers);
    }

    @Benchmark
    public Component globalPlaceholders() {
        return this.adventure.format(GLOBAL, null, Placeholder.empty());
    }

    @Benchmark
    public String legacyLocalPlaceholders() {
        return this.legacy.format(LEGACY, null, this.shop);
    }

    @Benchmark
    public String placeholderParse() {
        return this.shop.parse(LEGACY);
    }

    @Benchmark
    public String colorMiniMessage() {
        return AdventureMessageFormatter.colorMiniMessage(LEGACY);
    }

    @Benchmark
    public void broadcast100Players(Blackhole blackhole) {
        this.adventure.sendMessage(this.players, SHOP, false, this.shop);
        blackhole.consume(this.players);
    }
}
