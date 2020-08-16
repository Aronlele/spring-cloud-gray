package cn.springcloud.gray.server.dao.repository;

import cn.springcloud.gray.server.dao.model.RoutePolicyRecordDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface RoutePolicyRecordRepository extends JpaRepository<RoutePolicyRecordDO, Long>, JpaSpecificationExecutor<RoutePolicyRecordDO> {

    RoutePolicyRecordDO findFirstByTypeAndModuleIdAndResourceAndPolicyIdOrderByIdDesc(String type, String moduleId, String resource, Long policyId);

}
