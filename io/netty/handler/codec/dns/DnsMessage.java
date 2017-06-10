package io.netty.handler.codec.dns;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCountUtil;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class DnsMessage
  extends AbstractReferenceCounted
{
  private List<DnsQuestion> questions;
  private List<DnsResource> answers;
  private List<DnsResource> authority;
  private List<DnsResource> additional;
  private final DnsHeader header;
  
  DnsMessage(int id)
  {
    this.header = newHeader(id);
  }
  
  public DnsHeader header()
  {
    return this.header;
  }
  
  public List<DnsQuestion> questions()
  {
    if (this.questions == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.questions);
  }
  
  public List<DnsResource> answers()
  {
    if (this.answers == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.answers);
  }
  
  public List<DnsResource> authorityResources()
  {
    if (this.authority == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.authority);
  }
  
  public List<DnsResource> additionalResources()
  {
    if (this.additional == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(this.additional);
  }
  
  public DnsMessage addAnswer(DnsResource answer)
  {
    if (this.answers == null) {
      this.answers = new LinkedList();
    }
    this.answers.add(answer);
    return this;
  }
  
  public DnsMessage addQuestion(DnsQuestion question)
  {
    if (this.questions == null) {
      this.questions = new LinkedList();
    }
    this.questions.add(question);
    return this;
  }
  
  public DnsMessage addAuthorityResource(DnsResource resource)
  {
    if (this.authority == null) {
      this.authority = new LinkedList();
    }
    this.authority.add(resource);
    return this;
  }
  
  public DnsMessage addAdditionalResource(DnsResource resource)
  {
    if (this.additional == null) {
      this.additional = new LinkedList();
    }
    this.additional.add(resource);
    return this;
  }
  
  protected void deallocate() {}
  
  public boolean release()
  {
    release(questions());
    release(answers());
    release(additionalResources());
    release(authorityResources());
    return super.release();
  }
  
  private static void release(List<?> resources)
  {
    for (Object resource : resources) {
      ReferenceCountUtil.release(resource);
    }
  }
  
  public boolean release(int decrement)
  {
    release(questions(), decrement);
    release(answers(), decrement);
    release(additionalResources(), decrement);
    release(authorityResources(), decrement);
    return super.release(decrement);
  }
  
  private static void release(List<?> resources, int decrement)
  {
    for (Object resource : resources) {
      ReferenceCountUtil.release(resource, decrement);
    }
  }
  
  public DnsMessage touch(Object hint)
  {
    touch(questions(), hint);
    touch(answers(), hint);
    touch(additionalResources(), hint);
    touch(authorityResources(), hint);
    return this;
  }
  
  private static void touch(List<?> resources, Object hint)
  {
    for (Object resource : resources) {
      ReferenceCountUtil.touch(resource, hint);
    }
  }
  
  public DnsMessage retain()
  {
    retain(questions());
    retain(answers());
    retain(additionalResources());
    retain(authorityResources());
    super.retain();
    return this;
  }
  
  private static void retain(List<?> resources)
  {
    for (Object resource : resources) {
      ReferenceCountUtil.retain(resource);
    }
  }
  
  public DnsMessage retain(int increment)
  {
    retain(questions(), increment);
    retain(answers(), increment);
    retain(additionalResources(), increment);
    retain(authorityResources(), increment);
    super.retain(increment);
    return this;
  }
  
  private static void retain(List<?> resources, int increment)
  {
    for (Object resource : resources) {
      ReferenceCountUtil.retain(resource, increment);
    }
  }
  
  public DnsMessage touch()
  {
    super.touch();
    return this;
  }
  
  protected abstract DnsHeader newHeader(int paramInt);
}
