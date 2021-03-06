# Reflection之利用反射分析类的能力

简要介绍一下反射机制最重要的内容-检查类的结构

在java.lang.reflect包中有三个类Field、Method和Constructor分别用于描述类的域、方法和构造器。这个三个类都有一个叫做getName的方法，用来返回项目的名称。

Field类有一个getType方法，用来返回描述所属类型的Class对象。

Method和Constructor类有能够报告参数类型的方法，Method类还有一个可以报告返回类型的方法。

这个三个类还有一个叫做getModifiers的方法，它将返回一个整型数值，用不同的位开关描述public和static这样的修饰符使用状况。

还可以利用java.lang.reflect包中的Modifier类的静态方法分析getModifiers返回的整型数值。例如，可以使用Modifier类中的isPublic、isPrivate或isFinal判断方法或构造器是否是public、private或final。我们需要做的全部工作就是调用Modifier类的相应方法，并对返回的整型数值进行分析，另外，还可以利用Modifier.toString方法将修饰法打印出来。

Class类中的getFields、getMethods和getConstructors方法将分别返回类提供的public域、方法和构造器数组，其中包括超类的共有成员。

Class类的getDeclareFields、getDeclareMethods和getDeclaredConstructors方法将分别放回类中声明的全部域、方法和构造器，其中包括私有和受保护成员，但不包括超类的成员。

