jpa 对于各种数据源提供了统一的抽象和模板方法
感觉还是非常优雅的

数据源

1. database
2. mongo
3. redis
4. es

统一的抽象就是 Repository 所有的数据源都是 Repository

瞄一眼这些模板方法
CrudRepository<T,K>
ElasticsearchRepository<T,K>
MongoRepository<T,K>
