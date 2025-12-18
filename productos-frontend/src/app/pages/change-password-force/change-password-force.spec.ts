import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ChangePasswordForce } from './change-password-force';

describe('ChangePasswordForce', () => {
  let component: ChangePasswordForce;
  let fixture: ComponentFixture<ChangePasswordForce>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ChangePasswordForce]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ChangePasswordForce);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
