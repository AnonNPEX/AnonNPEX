package org.nutz.dao.impl.entity.field;

import org.nutz.dao.Cnd;
import org.nutz.dao.Condition;
import org.nutz.dao.Sqls;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.LinkType;
import org.nutz.dao.entity.PkType;
import org.nutz.dao.impl.EntityHolder;
import org.nutz.dao.impl.entity.EntityName;
import org.nutz.dao.impl.entity.info.LinkInfo;
import org.nutz.dao.util.cri.SimpleCriteria;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;

public class ManyManyLinkField extends AbstractLinkField {

    private EntityName relationTableName;

    private String fromColumnName;

    private String toColumnName;
    
    public ManyManyLinkField(Entity<?> host, EntityHolder holder, LinkInfo info, Class<?> klass, String from, String to, String relation, String key) {
    	super(host, holder, info);
    	this.targetType = klass;
        this.mapKey = key;
        this.relationTableName = EntityName.create(relation);

        String[] ss = Strings.splitIgnoreBlank(from, ":");
        this.fromColumnName = ss[0];
        String fromField = ss.length > 1 ? ss[1] : null;

        ss = Strings.splitIgnoreBlank(to, ":");
        this.toColumnName = ss[0];
        String toField = ss.length > 1 ? ss[1] : null;
        _make(host, fromField, toField);
	}

    public ManyManyLinkField(Entity<?> host, EntityHolder holder, LinkInfo info) {
        super(host, holder, info);
        this.targetType = guessTargetClass(info, info.manymany.target());
        this.mapKey = info.manymany.key();
        this.relationTableName = EntityName.create(info.manymany.relation());

        String[] ss = Strings.splitIgnoreBlank(info.manymany.from(), ":");
        this.fromColumnName = ss[0];
        String fromField = ss.length > 1 ? ss[1] : null;

        ss = Strings.splitIgnoreBlank(info.manymany.to(), ":");
        this.toColumnName = ss[0];
        String toField = ss.length > 1 ? ss[1] : null;
        _make(host, fromField, toField);
    }
    protected void _make(Entity<?> host, String fromField, String toField) {
        /*
         * ???????????????????????????????????????
         */
        Entity<?> ta = this.getLinkedEntity();

        // ??????????????? "from" ??? Java ?????????
        if (fromField != null) {
            hostField = host.getField(fromField);
            if (hostField == null) {
                // ?????????from????????????,?????????????!!!
                throw Lang.makeThrow(    "@ManyMany(from='%s') is invalid, no such field!! Host class=%s",
                                         fromField,
                                         host.getType().getName());
            }
        }
        // ??????????????? "to" ??? Java ?????????
        if (null != toField) {
            linkedField = ta.getField(toField);
            if (linkedField == null) {
                // ?????????from????????????,?????????????!!!
                throw Lang.makeThrow(    "@ManyMany(to='%s') is invalid, no such field!! Host class=%s",
                                         toField,
                                         host.getType().getName());
            }
        }

        // ????????????????????? "from" ??? Java ??????
        if (null != hostField && linkedField == null) {
            linkedField = ta.getPkType() == PkType.ID ? ta.getIdField() : ta.getNameField();
        }
        // ????????????????????? "to" ??? Java ??????
        else if (null == hostField && linkedField != null) {
            hostField = host.getPkType() == PkType.ID ? host.getIdField() : host.getNameField();
        }
        // ??????????????????????????? Id ???????????????
        else {
            // ?????? ID
            if (null != host.getIdField() && null != ta.getIdField()) {
                hostField = host.getIdField();
                linkedField = ta.getIdField();
            }
            // ??????ID?????? Name
            else if (null != host.getIdField() && null != ta.getNameField()) {
                hostField = host.getIdField();
                linkedField = ta.getNameField();
            }
            // ??????Name ??? ID
            else if (null != host.getNameField() && null != ta.getIdField()) {
                hostField = host.getNameField();
                linkedField = ta.getIdField();
            }
            // ?????? Name
            else if (null != host.getNameField() && null != ta.getNameField()) {
                hostField = host.getNameField();
                linkedField = ta.getNameField();
            }

        }
        // ????????????????????? ...
        if (null == hostField) {
            throw Lang.makeThrow(    "@ManyMany at [%s#%s] is Invalid: lack @Id or @Name at class=%s",
                                     host.getType().getName(),
                                     getName(),
                                     host.getType().getName());
        }
        if (null == linkedField) {
            throw Lang.makeThrow(    "@ManyMany at [%s#%s] is Invalid: lack @Id or @Name at class=%s",
                                     host.getType().getName(),
                                     getName(),
                                     target.getType().getName());
        }

    }

    public Condition createCondition(Object host) {
        SimpleCriteria cri = Cnd.cri();
        cri.where().andInBySql(    linkedField.getColumnName(),
                                "SELECT %s FROM %s WHERE %s=%s",
                                toColumnName,
                                this.getRelationName(),
                                fromColumnName,
                                Sqls.formatFieldValue(hostField.getValue(host)));
        return cri;
    }

    public void updateLinkedField(Object obj, Object linked) {}

    public void saveLinkedField(Object obj, Object linked) {}

    public LinkType getLinkType() {
        return LinkType.MANYMANY;
    }

    public String getRelationName() {
        return this.relationTableName.value();
    }

    public String getFromColumnName() {
        return fromColumnName;
    }

    public String getToColumnName() {
        return toColumnName;
    }

    /**
     * ????????????????????????????????? Java ???????????????
     * <p>
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????
     * 
     * @return ??????????????????????????? Java ???????????????
     */
    public String[] getLinkedPkNames() {
        String[] re = new String[2];
        re[0] = hostField.getName();
        re[1] = linkedField.getName();
        return re;
    }

}
